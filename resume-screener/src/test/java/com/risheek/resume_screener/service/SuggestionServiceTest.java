package com.risheek.resume_screener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.dto.JobSuggestionResponse;
import com.risheek.resume_screener.dto.SuggestionResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.Score;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.JobSkillRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.ScoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

    @Mock private ScoreRepository scoreRepository;
    @Mock private ResumeRepository resumeRepository;
    @Mock private JobRepository jobRepository;
    @Mock private JobSkillRepository jobSkillRepository;
    @Mock private WebClient.Builder webClientBuilder;

    private WebClient webClient;
    private SuggestionService suggestionService;

    private static final String EMAIL = "test@example.com";
    private static final Long RESUME_ID = 100L;
    private static final Long JOB_ID = 50L;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        suggestionService = new SuggestionService(
                scoreRepository, resumeRepository, jobRepository,
                jobSkillRepository, new ObjectMapper(), webClientBuilder, "http://localhost:8000"
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
        job.setDescription("Build and maintain REST APIs");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0));
        return job;
    }

    private Resume buildResume(User user) {
        Resume resume = new Resume();
        resume.setId(RESUME_ID);
        resume.setUser(user);
        resume.setParsedText("Experienced Java developer with Spring Boot background");
        return resume;
    }

    private Score buildScore(String matched, String missing, BigDecimal overall) {
        Score score = new Score();
        score.setOverallScore(overall);
        score.setMatchedKeywords(matched);
        score.setMissingKeywords(missing);
        return score;
    }

    @Test
    void getImprovementSuggestions_happyPath_buildsResponseFromMlFields() {
        User user = buildUser();
        Job job = buildJob();
        Resume resume = buildResume(user);
        Score score = buildScore("[\"Java\"]", "[\"Kubernetes\"]", new BigDecimal("75"));

        JobSkill skill = new JobSkill();
        skill.setSkillName("Java");

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));
        when(jobSkillRepository.findByJobId(JOB_ID)).thenReturn(List.of(skill));

        Map<String, Object> mlResponse = Map.of(
                "scoreLevel", "GOOD",
                "weakAreas", List.of("System Design"),
                "actionableSteps", List.of("Practice mock interviews"),
                "suggestedLearningPaths", List.of("Educative System Design course"),
                "resumeImprovementTips", List.of("Quantify your impact with metrics")
        );
        when(webClient.post().uri("/suggest").bodyValue(any()).retrieve()
                .bodyToMono(Map.class).block()).thenReturn(mlResponse);

        SuggestionResponse result = suggestionService.getImprovementSuggestions(RESUME_ID, JOB_ID);

        assertThat(result.getResumeId()).isEqualTo(RESUME_ID);
        assertThat(result.getScoreLevel()).isEqualTo("GOOD");
        assertThat(result.getWeakAreas()).containsExactly("System Design");
        assertThat(result.getActionableSteps()).containsExactly("Practice mock interviews");
        assertThat(result.getSuggestedLearningPaths()).containsExactly("Educative System Design course");
        assertThat(result.getResumeImprovementTips()).containsExactly("Quantify your impact with metrics");
    }

    @Test
    void getImprovementSuggestions_resumeNotFound_throwsResumeNotFoundException() {
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.empty());

        assertThrows(ResumeNotFoundException.class,
                () -> suggestionService.getImprovementSuggestions(RESUME_ID, JOB_ID));
    }

    @Test
    void getImprovementSuggestions_wrongOwner_throwsUnauthorizedAccessException() {
        User owner = new User();
        owner.setId(99L);
        owner.setEmail("owner@test.com");

        Resume resume = buildResume(owner);
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));

        assertThrows(UnauthorizedAccessException.class,
                () -> suggestionService.getImprovementSuggestions(RESUME_ID, JOB_ID));
    }

    @Test
    void getImprovementSuggestions_jobNotFound_throwsJobNotFoundException() {
        User user = buildUser();
        Resume resume = buildResume(user);

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class,
                () -> suggestionService.getImprovementSuggestions(RESUME_ID, JOB_ID));
    }

    @Test
    void getImprovementSuggestions_scoreNotFound_throwsScoreNotFoundException() {
        User user = buildUser();
        Job job = buildJob();
        Resume resume = buildResume(user);

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.empty());

        assertThrows(ScoreNotFoundException.class,
                () -> suggestionService.getImprovementSuggestions(RESUME_ID, JOB_ID));
    }

    @Test
    void getImprovementSuggestions_mlFails_returnsStrongFallback() {
        User user = buildUser();
        Job job = buildJob();
        Resume resume = buildResume(user);
        Score score = buildScore("[\"Java\"]", "[\"Kubernetes\"]", new BigDecimal("85"));

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));
        when(jobSkillRepository.findByJobId(JOB_ID)).thenReturn(List.of());
        when(webClient.post().uri("/suggest").bodyValue(any()).retrieve()
                .bodyToMono(Map.class).block()).thenThrow(new RuntimeException("ML down"));

        SuggestionResponse result = suggestionService.getImprovementSuggestions(RESUME_ID, JOB_ID);

        assertThat(result.getScoreLevel()).isEqualTo("STRONG");
        assertThat(result.getWeakAreas()).anyMatch(s -> s.contains("ML service unavailable"));
        assertThat(result.getActionableSteps()).anyMatch(s -> s.contains("Kubernetes"));
    }

    @Test
    void getImprovementSuggestions_mlFails_returnsModerateFallback() {
        User user = buildUser();
        Job job = buildJob();
        Resume resume = buildResume(user);
        Score score = buildScore("[\"Java\"]", "[\"Kubernetes\"]", new BigDecimal("75"));

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));
        when(jobSkillRepository.findByJobId(JOB_ID)).thenReturn(List.of());
        when(webClient.post().uri("/suggest").bodyValue(any()).retrieve()
                .bodyToMono(Map.class).block()).thenThrow(new RuntimeException("ML down"));

        SuggestionResponse result = suggestionService.getImprovementSuggestions(RESUME_ID, JOB_ID);

        assertThat(result.getScoreLevel()).isEqualTo("MODERATE");
    }

    @Test
    void getImprovementSuggestions_mlFails_returnsWeakFallback() {
        User user = buildUser();
        Job job = buildJob();
        Resume resume = buildResume(user);
        Score score = buildScore("[\"Java\"]", "[\"Kubernetes\"]", new BigDecimal("40"));

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));
        when(jobSkillRepository.findByJobId(JOB_ID)).thenReturn(List.of());
        when(webClient.post().uri("/suggest").bodyValue(any()).retrieve()
                .bodyToMono(Map.class).block()).thenThrow(new RuntimeException("ML down"));

        SuggestionResponse result = suggestionService.getImprovementSuggestions(RESUME_ID, JOB_ID);

        assertThat(result.getScoreLevel()).isEqualTo("WEAK");
    }

    @Test
    void getSuggestedJobs_happyPath_mapsMlResultsCorrectly() {
        User user = buildUser();
        Resume resume = buildResume(user);
        Score score = buildScore("[\"Java\",\"Spring Boot\"]", "[]", new BigDecimal("80"));

        Job job = buildJob();
        JobSkill skill = new JobSkill();
        skill.setSkillName("Java");

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));
        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobSkillRepository.findByJobId(JOB_ID)).thenReturn(List.of(skill));

        Map<String, Object> mlResult = Map.of(
                "jobId", 50,
                "jobTitle", "Backend Developer",
                "jobType", "FULL_TIME",
                "experienceLevel", "MID",
                "matchPercentage", 92.5,
                "matchedSkills", List.of("Java"),
                "missingSkills", List.of("Docker"),
                "semanticSimilarity", 0.88,
                "whyMatch", "Strong Java alignment"
        );
        when(webClient.post().uri("/match-jobs").bodyValue(any()).retrieve()
                .bodyToFlux(Map.class).collectList().block()).thenReturn(List.of(mlResult));

        List<JobSuggestionResponse> results = suggestionService.getSuggestedJobs(RESUME_ID, JOB_ID);

        assertThat(results).hasSize(1);
        JobSuggestionResponse result = results.getFirst();
        assertThat(result.getJobId()).isEqualTo(50L);
        assertThat(result.getJobTitle()).isEqualTo("Backend Developer");
        assertThat(result.getMatchPercentage()).isEqualTo(92.5);
        assertThat(result.getMatchedSkills()).containsExactly("Java");
        assertThat(result.getMissingSkills()).containsExactly("Docker");
        assertThat(result.getSemanticSimilarity()).isEqualTo(0.88);
        assertThat(result.getWhyMatch()).isEqualTo("Strong Java alignment");
    }

    @Test
    void getSuggestedJobs_resumeNotFound_throwsResumeNotFoundException() {
        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.empty());

        assertThrows(ResumeNotFoundException.class,
                () -> suggestionService.getSuggestedJobs(RESUME_ID, JOB_ID));
    }

    @Test
    void getSuggestedJobs_wrongOwner_throwsUnauthorizedAccessException() {
        User owner = new User();
        owner.setEmail("owner@test.com");
        Resume resume = buildResume(owner);

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));

        assertThrows(UnauthorizedAccessException.class,
                () -> suggestionService.getSuggestedJobs(RESUME_ID, JOB_ID));
    }

    @Test
    void getSuggestedJobs_mlFails_fallbackFiltersZeroMatchesAndLimitsToFive() {
        User user = buildUser();
        Resume resume = buildResume(user);
        Score score = buildScore("[\"Java\",\"Spring\",\"Docker\"]", "[]", new BigDecimal("80"));

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));

        List<Job> jobs = new ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            Job j = new Job();
            j.setId(i);
            j.setTitle("Job " + i);
            j.setDescription("Description " + i);
            j.setJobType(Job.JobType.FULL_TIME);
            j.setExperienceLevel(Job.ExperienceLevel.MID);
            j.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0));
            j.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0));
            jobs.add(j);
        }
        when(jobRepository.findAll()).thenReturn(jobs);

        when(jobSkillRepository.findByJobId(1L)).thenReturn(List.of(skill("Java"), skill("Spring")));
        when(jobSkillRepository.findByJobId(2L)).thenReturn(List.of(skill("Java")));
        when(jobSkillRepository.findByJobId(3L)).thenReturn(List.of(skill("Spring")));
        when(jobSkillRepository.findByJobId(4L)).thenReturn(List.of(skill("Docker")));
        when(jobSkillRepository.findByJobId(5L)).thenReturn(List.of(skill("Java"), skill("Docker")));
        when(jobSkillRepository.findByJobId(6L)).thenReturn(List.of(skill("Python"))); // 0% match

        when(webClient.post().uri("/match-jobs").bodyValue(any()).retrieve()
                .bodyToFlux(Map.class).collectList().block()).thenThrow(new RuntimeException("ML offline"));

        List<JobSuggestionResponse> results = suggestionService.getSuggestedJobs(RESUME_ID, JOB_ID);

        assertThat(results.size()).isLessThanOrEqualTo(5);
        assertThat(results).noneMatch(r -> r.getJobId().equals(6L));
        assertThat(results).allMatch(r -> r.getWhyMatch()
                .equals("Match based on keyword analysis only (ML service offline)"));
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getMatchPercentage())
                    .isGreaterThanOrEqualTo(results.get(i + 1).getMatchPercentage());
        }
    }

    @Test
    void parseJsonList_malformedJson_returnsEmptyListWithoutThrowing() {
        User user = buildUser();
        Resume resume = buildResume(user);
        Score score = new Score();
        score.setMatchedKeywords("not-valid-json");

        when(resumeRepository.findById(RESUME_ID)).thenReturn(Optional.of(resume));
        when(scoreRepository.findByResumeIdAndJobId(RESUME_ID, JOB_ID)).thenReturn(Optional.of(score));
        when(jobRepository.findAll()).thenReturn(List.of());
        when(webClient.post().uri("/match-jobs").bodyValue(any()).retrieve()
                .bodyToFlux(Map.class).collectList().block()).thenThrow(new RuntimeException());

        assertThatCode(() -> suggestionService.getSuggestedJobs(RESUME_ID, JOB_ID))
                .doesNotThrowAnyException();

        List<JobSuggestionResponse> result = suggestionService.getSuggestedJobs(RESUME_ID, JOB_ID);
        assertThat(result).isEmpty();
    }

    private JobSkill skill(String name) {
        JobSkill skill = new JobSkill();
        skill.setSkillName(name);
        return skill;
    }
}
