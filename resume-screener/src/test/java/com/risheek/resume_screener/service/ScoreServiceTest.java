package com.risheek.resume_screener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.entity.*;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock private ScoreRepository scoreRepository;
    @Mock private UserRepository userRepository;
    @Mock private JobSkillRepository jobSkillRepository;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private ResumeRepository resumeRepository;
    @Mock private JobRepository jobRepository;

    private WebClient webClient;
    private ObjectMapper objectMapper;
    private ScoreService scoreService;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        objectMapper = new ObjectMapper();

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        scoreService = new ScoreService(
                scoreRepository, userRepository, jobSkillRepository,
                webClientBuilder, objectMapper, resumeRepository,  jobRepository, "http://fake-ml-service"
        );

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", null));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateScore_newScore_happyPath() {
        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setDescription("Looking for a Java backend developer");

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setParsedText("Experienced Java developer with Spring Boot skills");

        JobSkill skill1 = new JobSkill(null, job, "Java");
        JobSkill skill2 = new JobSkill(null, job, "Spring Boot");
        when(jobSkillRepository.findByJobId(10L)).thenReturn(List.of(skill1, skill2));

        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.empty());

        Map<String, Object> mlResponse = Map.of(
                "overallScore", 82.5,
                "matchedKeywords", List.of("Java", "Spring Boot"),
                "missingKeywords", List.of("Kubernetes"),
                "recommendationsSummary", "Strong match for backend role"
        );
        when(webClient.post()
                .uri("/analyze")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(Map.class)
                .block())
                .thenReturn(mlResponse);

        when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scoreService.generateScore(resume, job);

        ArgumentCaptor<Score> scoreCaptor = ArgumentCaptor.forClass(Score.class);
        verify(scoreRepository).save(scoreCaptor.capture());
        Score savedScore = scoreCaptor.getValue();

        assertThat(savedScore.getUser()).isEqualTo(user);
        assertThat(savedScore.getJob()).isEqualTo(job);
        assertThat(savedScore.getResume()).isEqualTo(resume);
        assertThat(savedScore.getOverallScore()).isEqualByComparingTo(BigDecimal.valueOf(82.5));
        assertThat(savedScore.getMatchedKeywords()).contains("Java", "Spring Boot");
        assertThat(savedScore.getMissingKeywords()).contains("Kubernetes");
        assertThat(savedScore.getRecommendationsSummary()).isEqualTo("Strong match for backend role");
    }

    @Test
    void generateScore_mlFailurePath() {
        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setDescription("Looking for a Java backend developer");

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setParsedText("Experienced Java developer with Spring Boot skills");

        JobSkill skill1 = new JobSkill(null, job, "Java");
        JobSkill skill2 = new JobSkill(null, job, "Spring Boot");
        when(jobSkillRepository.findByJobId(10L)).thenReturn(List.of(skill1, skill2));

        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.empty());

        when(webClient.post()
                .uri("/analyze")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(Map.class)
                .block())
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scoreService.generateScore(resume, job);

        ArgumentCaptor<Score> scoreCaptor = ArgumentCaptor.forClass(Score.class);
        verify(scoreRepository).save(scoreCaptor.capture());
        Score savedScore = scoreCaptor.getValue();

        assertThat(savedScore.getOverallScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedScore.getMatchedKeywords()).isEqualTo("[]");
        assertThat(savedScore.getMissingKeywords()).isEqualTo("[]");
        assertThat(savedScore.getRecommendationsSummary()).isEqualTo("Scoring unavailable — ML service is offline.");
    }

    @Test
    void generateScore_existingScore_upsertsInsteadOfCreatingNew() {
        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setDescription("Looking for a Java backend developer");

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setParsedText("Experienced Java developer");

        Score existingScore = new Score();
        existingScore.setId(500L);
        existingScore.setOverallScore(BigDecimal.valueOf(40.0));
        existingScore.setMatchedKeywords("[\"OldSkill\"]");

        when(jobSkillRepository.findByJobId(10L)).thenReturn(List.of());
        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.of(existingScore));

        Map<String, Object> mlResponse = Map.of(
                "overallScore", 90.0,
                "matchedKeywords", List.of("Java"),
                "missingKeywords", List.of(),
                "recommendationsSummary", "Updated match"
        );
        when(webClient.post().uri("/analyze").bodyValue(any()).retrieve()
                .bodyToMono(Map.class).block())
                .thenReturn(mlResponse);

        when(scoreRepository.save(any(Score.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scoreService.generateScore(resume, job);

        ArgumentCaptor<Score> scoreCaptor = ArgumentCaptor.forClass(Score.class);
        verify(scoreRepository).save(scoreCaptor.capture());
        Score savedScore = scoreCaptor.getValue();

        assertThat(savedScore).isSameAs(existingScore);
        assertThat(savedScore.getId()).isEqualTo(500L);
        assertThat(savedScore.getOverallScore()).isEqualByComparingTo(BigDecimal.valueOf(90.0));
        assertThat(savedScore.getMatchedKeywords()).contains("Java");
        assertThat(savedScore.getJob()).isEqualTo(job);
        assertThat(savedScore.getUser()).isEqualTo(user);
    }

    @Test
    void getScoreByResume_happyPath() {

        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);

        Score score = new Score();
        score.setUser(user);
        score.setJob(job);
        score.setResume(resume);
        score.setOverallScore(BigDecimal.valueOf(85.50));
        score.setMatchedKeywords("Java");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.of(score));

        ScoreResponse response = scoreService.getScoreByResume(100L, 10L);

        assertThat(response.getResumeId()).isEqualTo(resume.getId());
        assertThat(response.getOverallScore()).isEqualTo(BigDecimal.valueOf(85.50));
        assertThat(response.getMatchedKeywords()).contains("Java");
    }

    @Test
    void getScoreByResume_unauthorized() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("test@example.com");

        User owner = new User();
        owner.setId(2L);

        Job job = new Job();
        job.setId(10L);

        Resume resume = new Resume();
        resume.setUser(owner);
        resume.setId(10L);

        Score score = new Score();
        score.setUser(owner);
        score.setJob(job);
        score.setResume(resume);
        score.setOverallScore(BigDecimal.valueOf(85.50));
        score.setMatchedKeywords("Java");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(scoreRepository.findByResumeIdAndJobId(10L, 10L)).thenReturn(Optional.of(score));

        assertThrows(UnauthorizedAccessException.class,
                () -> scoreService.getScoreByResume(10L, 10L));
    }

    @Test
    void getScoreByResume_notFound() {
        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.empty());

        assertThrows(ScoreNotFoundException.class,
                () -> scoreService.getScoreByResume(100L, 10L));
    }

    @Test
    void getMyScores_happyPath() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);

        Resume resume1 = new Resume();
        resume1.setId(10001L);

        Resume resume2 = new Resume();
        resume2.setId(1000002L);

        Score score1 = new Score();
        score1.setId(100L);
        score1.setJob(job);
        score1.setResume(resume1);
        score1.setUser(user);
        score1.setOverallScore(BigDecimal.valueOf(26));
        score1.setMatchedKeywords("JAVA");
        score1.setMissingKeywords("AWS");
        score1.setRecommendationsSummary("yes you can do it if you study");

        Score score2 = new Score();
        score2.setId(101L);
        score2.setJob(job);
        score2.setResume(resume2);
        score2.setUser(user);
        score2.setOverallScore(BigDecimal.valueOf(85));
        score2.setMatchedKeywords("JAVA, PYTHON");
        score2.setMissingKeywords("SQL");
        score2.setRecommendationsSummary("yes you can do it surely");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(scoreRepository.findByUserId(1L)).thenReturn(List.of(score1, score2));

        List<ScoreResponse> response = scoreService.getMyScores();

        assertThat(response.size()).isEqualTo(2);
        assertThat(response.get(0).getId()).isEqualTo(100L);
        assertThat(response.get(0).getMatchedKeywords()).contains("JAVA");
        assertThat(response.get(1).getId()).isEqualTo(101L);
        assertThat(response.get(1).getMissingKeywords()).contains("SQL");
    }

    @Test
    void getMyScores_emptyList() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(scoreRepository.findByUserId(1L)).thenReturn(List.of());

        List<ScoreResponse> response = scoreService.getMyScores();

        assertThat(response.size()).isEqualTo(0);
    }

    @Test
    void generateScoreByIds_happyPath() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);
        job.setDescription("Java Backend");

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setParsedText("Java Spring Boot");

        JobSkill skill = new JobSkill(null, job, "Java");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of(skill));

        when(scoreRepository.findByResumeIdAndJobId(100L, 10L))
                .thenReturn(Optional.empty());

        Map<String, Object> mlResponse = Map.of(
                "overallScore", 88.0,
                "matchedKeywords", List.of("Java"),
                "missingKeywords", List.of("Docker"),
                "recommendationsSummary", "Good Match"
        );

        when(webClient.post()
                .uri("/analyze")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(Map.class)
                .block())
                .thenReturn(mlResponse);

        when(scoreRepository.save(any(Score.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScoreResponse response = scoreService.generateScore(100L, 10L);

        assertEquals(100L, response.getResumeId());
        assertEquals(10L, response.getJobId());
        assertEquals(BigDecimal.valueOf(88.0), response.getOverallScore());
    }

    @Test
    void generateScoreByIds_resumeNotFound() {

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResumeNotFoundException.class,
                () -> scoreService.generateScore(100L, 10L)
        );
    }

    @Test
    void generateScoreByIds_jobNotFound() {

        User user = new User();
        user.setId(1L);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(jobRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                JobNotFoundException.class,
                () -> scoreService.generateScore(100L, 10L)
        );
    }

    @Test
    void generateScoreByIds_unauthorizedResume() {

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("test@example.com");

        User owner = new User();
        owner.setId(2L);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(owner);

        Job job = new Job();
        job.setId(10L);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(currentUser));

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        assertThrows(
                UnauthorizedAccessException.class,
                () -> scoreService.generateScore(100L, 10L)
        );
    }
}
