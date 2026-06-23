package com.risheek.resume_screener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.dto.JobSuggestionResponse;
import com.risheek.resume_screener.dto.SuggestionResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.Score;
import com.risheek.resume_screener.entity.User;
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

    @Mock
    private ScoreRepository scoreRepository;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobSkillRepository jobSkillRepository;
    @Mock
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;
    private SuggestionService suggestionService;

    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        suggestionService = new SuggestionService(
                scoreRepository,
                resumeRepository,
                jobRepository,
                jobSkillRepository,
                new ObjectMapper(),
                webClientBuilder,
                "http://localhost:8000"
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

    @Test
    void getImprovementSuggestions_happyPath_buildsResponseFromMlFields() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);

        Job job = new Job();
        job.setUser(user);
        job.setId(50L);
        job.setTitle("Backend Developer");
        job.setDescription("Build and maintain REST APIs");

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setJob(job);
        resume.setParsedText("Experienced Java developer with Spring Boot background");

        Score score = new Score();
        score.setOverallScore(new BigDecimal("75"));
        score.setMatchedKeywords("[\"Java\"]");
        score.setMissingKeywords("[\"Kubernetes\"]");

        JobSkill skill = new JobSkill();
        skill.setSkillName("Java");

        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resume));
        when(scoreRepository.findByResumeId(100L)).thenReturn(Optional.of(score));
        when(jobSkillRepository.findByJobId(50L)).thenReturn(List.of(skill));

        Map<String, Object> mlResponse = Map.of(
                "scoreLevel", "GOOD",
                "weakAreas", List.of("System Design"),
                "actionableSteps", List.of("Practice mock interviews"),
                "suggestedLearningPaths", List.of("Educative System Design course"),
                "resumeImprovementTips", List.of("Quantify your impact with metrics")
        );

        when(webClient.post()
                .uri("/suggest")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(Map.class)
                .block())
                .thenReturn(mlResponse);

        SuggestionResponse result = suggestionService.getImprovementSuggestions(100L);

        assertThat(result.getResumeId()).isEqualTo(100L);
        assertThat(result.getScoreLevel()).isEqualTo("GOOD");
        assertThat(result.getWeakAreas()).containsExactly("System Design");
        assertThat(result.getActionableSteps()).containsExactly("Practice mock interviews");
        assertThat(result.getSuggestedLearningPaths()).containsExactly("Educative System Design course");
        assertThat(result.getResumeImprovementTips()).containsExactly("Quantify your impact with metrics");
    }

    @Test
    void getImprovementSuggestions_resumeNotFound_throwsResumeNotFoundException() {
        when(resumeRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResumeNotFoundException.class,
                () -> suggestionService.getImprovementSuggestions(100L));
    }

    @Test
    void getImprovementSuggestions_wrongOwner_throwsUnauthorizedAccessException() {

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@test.com");

        Job job = new Job();
        job.setId(50L);
        job.setUser(owner);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(owner);
        resume.setJob(job);
        resume.setParsedText("Experienced Java developer");

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        assertThrows(
                UnauthorizedAccessException.class,
                () -> suggestionService.getImprovementSuggestions(100L)
        );
    }

    @Test
    void getImprovementSuggestions_scoreNotFound_throwsScoreNotFoundException(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(50L);
        job.setUser(user);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setJob(job);
        resume.setParsedText("Experienced Java developer");

        Score score = new Score();
        score.setOverallScore(new BigDecimal("75"));
        score.setMatchedKeywords("[\"Java\"]");
        score.setMissingKeywords("[\"Kubernetes\"]");

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));
        when(scoreRepository.findByResumeId(100L)).thenReturn(Optional.empty());

        assertThrows(
                ScoreNotFoundException.class,
                () -> suggestionService.getImprovementSuggestions(100L)
        );
    }

    @Test
    void getImprovementSuggestions_mlFails_returnsStrongFallback() {

        Resume resume = createResume();

        Score score = new Score();
        score.setOverallScore(new BigDecimal("85"));
        score.setMatchedKeywords("[\"Java\"]");
        score.setMissingKeywords("[\"Kubernetes\"]");

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(scoreRepository.findByResumeId(100L))
                .thenReturn(Optional.of(score));

        when(webClient.post()
                .uri("/suggest")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(Map.class)
                .block())
                .thenThrow(new RuntimeException("ML service down"));

        SuggestionResponse result =
                suggestionService.getImprovementSuggestions(100L);

        assertThat(result.getScoreLevel()).isEqualTo("STRONG");

        assertThat(result.getWeakAreas())
                .anyMatch(s -> s.contains("ML service unavailable"));

        assertThat(result.getActionableSteps())
                .anyMatch(s -> s.contains("Kubernetes"));
    }

    @Test
    void getImprovementSuggestions_mlFails_returnsModerateFallback() {

        Resume resume = createResume();

        Score score = new Score();
        score.setOverallScore(new BigDecimal("75"));
        score.setMatchedKeywords("[\"Java\"]");
        score.setMissingKeywords("[\"Kubernetes\"]");

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(scoreRepository.findByResumeId(100L))
                .thenReturn(Optional.of(score));

        when(webClient.post()
                .uri("/suggest")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(Map.class)
                .block())
                .thenThrow(new RuntimeException("ML service down"));

        SuggestionResponse result =
                suggestionService.getImprovementSuggestions(100L);

        assertThat(result.getScoreLevel()).isEqualTo("MODERATE");

        assertThat(result.getWeakAreas())
                .anyMatch(s -> s.contains("ML service unavailable"));

        assertThat(result.getActionableSteps())
                .anyMatch(s -> s.contains("Kubernetes"));
    }

    @Test
    void getImprovementSuggestions_mlFails_returnsWeakFallback() {

        Resume resume = createResume();

        Score score = new Score();
        score.setOverallScore(new BigDecimal("40"));
        score.setMatchedKeywords("[\"Java\"]");
        score.setMissingKeywords("[\"Kubernetes\"]");

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(scoreRepository.findByResumeId(100L))
                .thenReturn(Optional.of(score));

        when(webClient.post()
                .uri("/suggest")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(Map.class)
                .block())
                .thenThrow(new RuntimeException("ML service down"));

        SuggestionResponse result =
                suggestionService.getImprovementSuggestions(100L);

        assertThat(result.getScoreLevel()).isEqualTo("WEAK");

        assertThat(result.getWeakAreas())
                .anyMatch(s -> s.contains("ML service unavailable"));

        assertThat(result.getActionableSteps())
                .anyMatch(s -> s.contains("Kubernetes"));
    }

    @Test
    void getSuggestedJobs_happyPath_mapsMlResultsCorrectly() {

        User user = new User();
        user.setEmail(EMAIL);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setParsedText("Java Spring Boot Developer");

        Score score = new Score();
        score.setMatchedKeywords("[\"Java\",\"Spring Boot\"]");

        Job job = new Job();
        job.setId(1L);
        job.setTitle("Backend Developer");
        job.setDescription("Backend API development");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);

        JobSkill skill = new JobSkill();
        skill.setSkillName("Java");

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(scoreRepository.findByResumeId(100L))
                .thenReturn(Optional.of(score));

        when(jobRepository.findAll())
                .thenReturn(List.of(job));

        when(jobSkillRepository.findByJobId(1L))
                .thenReturn(List.of(skill));

        Map<String, Object> mlResult = Map.of(
                "jobId", 1,
                "jobTitle", "Backend Developer",
                "jobType", "FULL_TIME",
                "experienceLevel", "MID",
                "matchPercentage", 92.5,
                "matchedSkills", List.of("Java"),
                "missingSkills", List.of("Docker"),
                "semanticSimilarity", 0.88,
                "whyMatch", "Strong Java alignment"
        );

        when(webClient.post()
                .uri("/match-jobs")
                .bodyValue(any())
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block())
                .thenReturn(List.of(mlResult));

        List<JobSuggestionResponse> results =
                suggestionService.getSuggestedJobs(100L);

        assertThat(results).hasSize(1);

        JobSuggestionResponse result = results.getFirst();

        assertThat(result.getJobId()).isEqualTo(1L);
        assertThat(result.getJobTitle()).isEqualTo("Backend Developer");
        assertThat(result.getJobType()).isEqualTo("FULL_TIME");
        assertThat(result.getExperienceLevel()).isEqualTo("MID");
        assertThat(result.getMatchPercentage()).isEqualTo(92.5);
        assertThat(result.getMatchedSkills()).containsExactly("Java");
        assertThat(result.getMissingSkills()).containsExactly("Docker");
        assertThat(result.getSemanticSimilarity()).isEqualTo(0.88);
        assertThat(result.getWhyMatch()).isEqualTo("Strong Java alignment");
    }

    @Test
    void getSuggestedJobs_resumeNotFound_throwsResumeNotFoundException() {

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResumeNotFoundException.class,
                () -> suggestionService.getSuggestedJobs(100L)
        );
    }

    @Test
    void getSuggestedJobs_wrongOwner_throwsUnauthorizedAccessException() {

        User owner = new User();
        owner.setEmail("owner@test.com");

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(owner);

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        assertThrows(
                UnauthorizedAccessException.class,
                () -> suggestionService.getSuggestedJobs(100L)
        );
    }

    @Test
    void getSuggestedJobs_mlFails_fallsBackAndFiltersZeroMatchesLimitsToFive() {

        User user = new User();
        user.setEmail(EMAIL);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setParsedText("Java Spring Docker");

        Score score = new Score();
        score.setMatchedKeywords("[\"Java\",\"Spring\",\"Docker\"]");

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(scoreRepository.findByResumeId(100L))
                .thenReturn(Optional.of(score));

        List<Job> jobs = new ArrayList<>();

        for (long i = 1; i <= 6; i++) {
            Job job = new Job();
            job.setId(i);
            job.setTitle("Job " + i);
            job.setDescription("Description " + i);
            job.setJobType(Job.JobType.FULL_TIME);
            job.setExperienceLevel(Job.ExperienceLevel.MID);
            jobs.add(job);
        }

        when(jobRepository.findAll()).thenReturn(jobs);

        when(jobSkillRepository.findByJobId(1L))
                .thenReturn(List.of(skill("Java"), skill("Spring")));

        when(jobSkillRepository.findByJobId(2L))
                .thenReturn(List.of(skill("Java")));

        when(jobSkillRepository.findByJobId(3L))
                .thenReturn(List.of(skill("Spring")));

        when(jobSkillRepository.findByJobId(4L))
                .thenReturn(List.of(skill("Docker")));

        when(jobSkillRepository.findByJobId(5L))
                .thenReturn(List.of(skill("Java"), skill("Docker")));

        when(jobSkillRepository.findByJobId(6L))
                .thenReturn(List.of(skill("Python"))); // 0%

        when(webClient.post()
                .uri("/match-jobs")
                .bodyValue(any())
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block())
                .thenThrow(new RuntimeException("ML offline"));

        List<JobSuggestionResponse> results =
                suggestionService.getSuggestedJobs(100L);

        assertThat(results.size()).isLessThanOrEqualTo(5);

        assertThat(results)
                .allMatch(r ->
                        r.getWhyMatch().equals(
                                "Match based on keyword analysis only (ML service offline)"
                        ));

        assertThat(results)
                .noneMatch(r -> r.getJobId().equals(6L));

        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getMatchPercentage())
                    .isGreaterThanOrEqualTo(
                            results.get(i + 1).getMatchPercentage()
                    );
        }
    }

    @Test
    void parseJsonList_malformedJson_returnsEmptyListNotException() {

        User user = new User();
        user.setEmail(EMAIL);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setParsedText("Java Developer");

        Score score = new Score();

        score.setMatchedKeywords("not-valid-json");

        when(resumeRepository.findById(100L))
                .thenReturn(Optional.of(resume));

        when(scoreRepository.findByResumeId(100L))
                .thenReturn(Optional.of(score));

        when(jobRepository.findAll())
                .thenReturn(List.of());

        when(webClient.post()
                .uri("/match-jobs")
                .bodyValue(any())
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block())
                .thenThrow(new RuntimeException());

        assertThatCode(() ->
                suggestionService.getSuggestedJobs(100L))
                .doesNotThrowAnyException();

        List<JobSuggestionResponse> result =
                suggestionService.getSuggestedJobs(100L);

        assertThat(result).isEmpty();
    }


    private Resume createResume() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);

        Job job = new Job();
        job.setId(50L);
        job.setUser(user);
        job.setTitle("Backend Developer");
        job.setDescription("Build and maintain REST APIs");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);
        resume.setJob(job);
        resume.setParsedText("Experienced Java developer");

        return resume;
    }

    private JobSkill skill(String name) {
        JobSkill skill = new JobSkill();
        skill.setSkillName(name);
        return skill;
    }

}