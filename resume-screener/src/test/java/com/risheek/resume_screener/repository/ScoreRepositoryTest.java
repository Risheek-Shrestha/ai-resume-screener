package com.risheek.resume_screener.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class ScoreRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private ScoreRepository scoreRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private JobSkillRepository jobSkillRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TestEntityManager entityManager;

    private User savedUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("password@1234");
        user.setRole(User.Role.USER);
        return userRepository.save(user);
    }

    private Job savedJob(User owner) {
        Job job = new Job();
        job.setTitle("DevOps Engineer");
        job.setDescription("Looking for a DevOps engineer with Docker and Kubernetes skills.");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(owner);
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0));
        return jobRepository.save(job);
    }

    private Resume savedResume(User user) {
        Resume resume = new Resume();
        resume.setUser(user);
        resume.setResumeName("Backend Developer Resume");
        resume.setFileName("test-resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("test content".getBytes());
        return resumeRepository.save(resume);
    }

    @Test
    void shouldGenerateAndRetrieveScore() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        User currentUser = savedUser("Risheek", "risheekshrestha@gmail.com");
        Job currentJob = savedJob(currentUser);
        Resume savedResume = savedResume(currentUser);

        Score score = new Score();
        score.setUser(currentUser);
        score.setJob(currentJob);
        score.setResume(savedResume);
        score.setOverallScore(BigDecimal.valueOf(85.5));
        score.setMatchedKeywords("[\"Docker\",\"Kubernetes\"]");
        score.setMissingKeywords("[\"AWS\"]");
        score.setRecommendationsSummary("Strong match on core DevOps tools.");

        Score savedScore = scoreRepository.save(score);
        entityManager.flush();
        entityManager.clear();

        Optional<Score> found = scoreRepository.findById(savedScore.getId());

        assertTrue(found.isPresent());
        assertEquals(0, BigDecimal.valueOf(85.5).compareTo(found.get().getOverallScore()));
        assertEquals(currentUser.getId(), found.get().getUser().getId());
        assertEquals(currentJob.getId(), found.get().getJob().getId());

        List<String> actualMatched = objectMapper.readValue(found.get().getMatchedKeywords(), List.class);
        assertEquals(List.of("Docker", "Kubernetes"), actualMatched);
    }

    @Test
    void shouldFindByResumeIdAndJobId() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        User currentUser = savedUser("Risheek", "risheekshrestha@gmail.com");
        Job currentJob = savedJob(currentUser);
        Resume savedResume = savedResume(currentUser);

        Score score = new Score();
        score.setUser(currentUser);
        score.setJob(currentJob);
        score.setResume(savedResume);
        score.setOverallScore(BigDecimal.valueOf(85.5));
        score.setMatchedKeywords("[\"Docker\",\"Kubernetes\"]");
        score.setMissingKeywords("[\"AWS\"]");
        score.setRecommendationsSummary("Strong match on core DevOps tools.");
        scoreRepository.save(score);

        entityManager.flush();
        entityManager.clear();

        Optional<Score> found = scoreRepository.findByResumeIdAndJobId(savedResume.getId(), currentJob.getId());

        assertTrue(found.isPresent());
        assertEquals(0, BigDecimal.valueOf(85.5).compareTo(found.get().getOverallScore()));

        List<String> actualMatched = objectMapper.readValue(found.get().getMatchedKeywords(), List.class);
        assertEquals(List.of("Docker", "Kubernetes"), actualMatched);
    }

    @Test
    void shouldFindByJobId() {
        User currentUser = savedUser("Risheek", "risheekshrestha@gmail.com");
        Job currentJob = savedJob(currentUser);
        Resume savedResume = savedResume(currentUser);

        Score score1 = new Score();
        score1.setUser(currentUser); score1.setJob(currentJob); score1.setResume(savedResume);
        score1.setOverallScore(BigDecimal.valueOf(85.5));
        score1.setMatchedKeywords("[\"Docker\"]"); score1.setMissingKeywords("[\"AWS\"]");
        score1.setRecommendationsSummary("Good");
        scoreRepository.save(score1);

        Score score2 = new Score();
        score2.setUser(currentUser); score2.setJob(currentJob); score2.setResume(savedResume);
        score2.setOverallScore(BigDecimal.valueOf(80.5));
        score2.setMatchedKeywords("[\"Kubernetes\"]"); score2.setMissingKeywords("[\"Python\"]");
        score2.setRecommendationsSummary("Decent");
        scoreRepository.save(score2);

        entityManager.flush();
        entityManager.clear();

        List<Score> scores = scoreRepository.findByJobId(currentJob.getId());

        assertEquals(2, scores.size());
        assertTrue(scores.stream().anyMatch(s -> BigDecimal.valueOf(85.5).compareTo(s.getOverallScore()) == 0));
        assertTrue(scores.stream().anyMatch(s -> BigDecimal.valueOf(80.5).compareTo(s.getOverallScore()) == 0));
    }

    @Test
    void shouldFindByUserId() {
        User userA = savedUser("Risheek", "risheekshrestha@gmail.com");
        User userB = savedUser("OtherUser", "otheruser@gmail.com");
        Job currentJob = savedJob(userA);
        Resume savedResume = savedResume(userA);

        Score score1 = new Score();
        score1.setUser(userA); score1.setJob(currentJob); score1.setResume(savedResume);
        score1.setOverallScore(BigDecimal.valueOf(85.5));
        score1.setMatchedKeywords("[\"Docker\"]"); score1.setMissingKeywords("[\"AWS\"]");
        score1.setRecommendationsSummary("Good");
        scoreRepository.save(score1);

        Score score2 = new Score();
        score2.setUser(userA); score2.setJob(currentJob); score2.setResume(savedResume);
        score2.setOverallScore(BigDecimal.valueOf(80.5));
        score2.setMatchedKeywords("[\"Kubernetes\"]"); score2.setMissingKeywords("[\"Python\"]");
        score2.setRecommendationsSummary("Decent");
        scoreRepository.save(score2);

        Score score3 = new Score();
        score3.setUser(userB); score3.setJob(currentJob); score3.setResume(savedResume);
        score3.setOverallScore(BigDecimal.valueOf(89.5));
        score3.setMatchedKeywords("[\"Java\"]"); score3.setMissingKeywords("[\"AWS\"]");
        score3.setRecommendationsSummary("Strong");
        scoreRepository.save(score3);

        entityManager.flush();
        entityManager.clear();

        List<Score> scores = scoreRepository.findByUserId(userA.getId());

        assertEquals(2, scores.size());
        assertTrue(scores.stream().anyMatch(s -> BigDecimal.valueOf(85.5).compareTo(s.getOverallScore()) == 0));
        assertTrue(scores.stream().anyMatch(s -> BigDecimal.valueOf(80.5).compareTo(s.getOverallScore()) == 0));
    }
}
