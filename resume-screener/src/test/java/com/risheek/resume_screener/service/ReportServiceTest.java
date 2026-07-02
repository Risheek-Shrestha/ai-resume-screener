package com.risheek.resume_screener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.dto.ReportResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.Score;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.ScoreRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private UserRepository userRepository;
    @Mock private JobRepository jobRepository;

    private ReportService reportService;

    private static final String EMAIL = "test@example.com";
    private static final Long RESUME_ID = 100L;
    private static final Long JOB_ID = 10L;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                resumeRepository, scoreRepository, userRepository,
                jobRepository, new ObjectMapper()
        );

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(EMAIL);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        return user;
    }

    private Job buildJob() {
        Job job = new Job();
        job.setId(JOB_ID);
        job.setTitle("Backend Developer");
        return job;
    }

    private Resume buildResume(User owner) {
        Resume resume = new Resume();
        resume.setId(RESUME_ID);
        resume.setUser(owner);
        return resume;
    }

    private Score buildScore(BigDecimal overallScore, String matchedJson, String missingJson) {
        Score score = new Score();
        score.setOverallScore(overallScore);
        score.setMatchedKeywords(matchedJson);
        score.setMissingKeywords(missingJson);
        return score;
    }

    @ParameterizedTest
    @CsvSource({
            "85, EXCELLENT, Interview Ready",
            "70, GOOD, Potential Candidate",
            "50, MODERATE, Needs Improvement",
            "20, WEAK, Not Ready"
    })
    void generateReport_happyPath_correctBandingAcrossScoreRanges(
            int scoreValue, String expectedLevel, String expectedReadiness) {

        User user = buildUser();
        Job job = buildJob();
        Resume resume = buildResume(user);
        Score score = buildScore(
                new BigDecimal(scoreValue),
                "[\"Java\",\"Spring Boot\"]",
                "[\"Kubernetes\",\"AWS\"]"
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));

        ReportResponse result = reportService.generateReport(RESUME_ID, JOB_ID);

        assertThat(result.getResumeId()).isEqualTo(RESUME_ID);
        assertThat(result.getJobTitle()).isEqualTo("Backend Developer");
        assertThat(result.getOverallScore()).isEqualTo(new BigDecimal(scoreValue));
        assertThat(result.getScoreLevel()).isEqualTo(expectedLevel);
        assertThat(result.getJobReadiness()).isEqualTo(expectedReadiness);
        assertThat(result.getStrengths()).containsExactly("Java", "Spring Boot");
        assertThat(result.getGaps()).containsExactly("Kubernetes", "AWS");
        assertThat(result.getImprovements()).containsExactly(
                "Add experience with Kubernetes",
                "Add experience with AWS"
        );
    }

    @Test
    void generateReport_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> reportService.generateReport(RESUME_ID, JOB_ID));
    }

    @Test
    void generateReport_resumeNotFound_throwsResumeNotFoundException() {
        User user = buildUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.empty());

        assertThrows(ResumeNotFoundException.class,
                () -> reportService.generateReport(RESUME_ID, JOB_ID));
    }

    @Test
    void generateReport_wrongOwner_throwsUnauthorizedAccessException() {
        User currentUser = buildUser();

        User otherOwner = new User();
        otherOwner.setId(999L);
        otherOwner.setEmail("someoneelse@example.com");

        Resume resume = buildResume(otherOwner);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(currentUser));
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));

        assertThrows(UnauthorizedAccessException.class,
                () -> reportService.generateReport(RESUME_ID, JOB_ID));
    }

    @Test
    void generateReport_jobNotFound_throwsJobNotFoundException() {
        User user = buildUser();
        Resume resume = buildResume(user);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class,
                () -> reportService.generateReport(RESUME_ID, JOB_ID));
    }

    @Test
    void generateReport_scoreNotFound_throwsScoreNotFoundException() {
        User user = buildUser();
        Job job = buildJob();
        Resume resume = buildResume(user);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.empty());

        assertThrows(ScoreNotFoundException.class,
                () -> reportService.generateReport(RESUME_ID, JOB_ID));
    }

    @Test
    void generateReport_emptyMissingSkills_fallsBackToGenericImprovements() {
        User user = buildUser();
        Job job = buildJob();
        Resume resume = buildResume(user);
        Score score = buildScore(new BigDecimal(75), "[\"Java\"]", "[]");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));

        ReportResponse result = reportService.generateReport(RESUME_ID, JOB_ID);

        assertThat(result.getImprovements()).containsExactly(
                "Continue building real-world projects",
                "Quantify project achievements",
                "Tailor resume to each application"
        );
    }
}
