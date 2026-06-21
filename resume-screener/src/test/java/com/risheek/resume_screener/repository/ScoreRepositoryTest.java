package com.risheek.resume_screener.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class ScoreRepositoryTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ScoreRepository scoreRepository;
    @Autowired
    private ResumeRepository resumeRepository;
    @Autowired
    private JobSkillRepository jobSkillRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldGenerateAndRetrieveScore() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        User user = new User();

        user.setUsername("Risheek");
        user.setEmail("risheekshrestha@gmail.com");
        user.setPasswordHash("risheek@1234");
        user.setRole(User.Role.USER);

        User currentUser =  userRepository.save(user);

        Job job = new Job();

        job.setTitle("DevOps Engineer");
        job.setDescription("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(currentUser);

        Job currentJob = jobRepository.save(job);

        JobSkill jobSkill1 = new JobSkill(null, currentJob, "Docker");
        JobSkill jobSkill2 = new JobSkill(null, currentJob, "Kubernetes");

        List<JobSkill> savedSkills = jobSkillRepository.saveAll(List.of(jobSkill1, jobSkill2));

        Resume resume = new Resume();
        resume.setUser(currentUser);
        resume.setJob(currentJob);
        resume.setFileName("test-resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("test content".getBytes());

        Resume savedResume = resumeRepository.save(resume);

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

        var found = scoreRepository.findById(savedScore.getId());

        assertTrue(found.isPresent());

        List<String> expectedMatched = List.of("Docker", "Kubernetes");
        List<String> actualMatched = objectMapper.readValue(found.get().getMatchedKeywords(), List.class);
        assertEquals(expectedMatched, actualMatched);

        List<String> expectedMissing = List.of("AWS");
        List<String> actualMissing = objectMapper.readValue(found.get().getMissingKeywords(), List.class);
        assertEquals(expectedMissing, actualMissing);

        assertEquals(0, BigDecimal.valueOf(85.5).compareTo(found.get().getOverallScore()));
        assertEquals(currentUser.getId(), found.get().getUser().getId());
        assertEquals(currentJob.getId(), found.get().getJob().getId());
    }

    @Test
    void shouldGenerateAndRetrieveScoreByResumeId() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        User user = new User();

        user.setUsername("Risheek");
        user.setEmail("risheekshrestha@gmail.com");
        user.setPasswordHash("risheek@1234");
        user.setRole(User.Role.USER);

        User currentUser =  userRepository.save(user);

        Job job = new Job();

        job.setTitle("DevOps Engineer");
        job.setDescription("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(currentUser);

        Job currentJob = jobRepository.save(job);

        JobSkill jobSkill1 = new JobSkill(null, currentJob, "Docker");
        JobSkill jobSkill2 = new JobSkill(null, currentJob, "Kubernetes");

        List<JobSkill> savedSkills = jobSkillRepository.saveAll(List.of(jobSkill1, jobSkill2));

        Resume resume = new Resume();
        resume.setUser(currentUser);
        resume.setJob(currentJob);
        resume.setFileName("test-resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("test content".getBytes());

        Resume savedResume = resumeRepository.save(resume);

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

        var found = scoreRepository.findByResumeId(savedResume.getId());

        assertTrue(found.isPresent());

        List<String> expectedMatched = List.of("Docker", "Kubernetes");
        List<String> actualMatched = objectMapper.readValue(found.get().getMatchedKeywords(), List.class);
        assertEquals(expectedMatched, actualMatched);

        List<String> expectedMissing = List.of("AWS");
        List<String> actualMissing = objectMapper.readValue(found.get().getMissingKeywords(), List.class);
        assertEquals(expectedMissing, actualMissing);

        assertEquals(0, BigDecimal.valueOf(85.5).compareTo(found.get().getOverallScore()));
        assertEquals(currentUser.getId(), found.get().getUser().getId());
        assertEquals(currentJob.getId(), found.get().getJob().getId());
    }

    @Test
    void shouldGenerateAndRetrieveScoreByJobId() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        User user = new User();

        user.setUsername("Risheek");
        user.setEmail("risheekshrestha@gmail.com");
        user.setPasswordHash("risheek@1234");
        user.setRole(User.Role.USER);

        User currentUser =  userRepository.save(user);

        Job job = new Job();

        job.setTitle("DevOps Engineer");
        job.setDescription("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(currentUser);

        Job currentJob = jobRepository.save(job);

        JobSkill jobSkill1 = new JobSkill(null, currentJob, "Docker");
        JobSkill jobSkill2 = new JobSkill(null, currentJob, "Kubernetes");

        List<JobSkill> savedSkills = jobSkillRepository.saveAll(List.of(jobSkill1, jobSkill2));

        Resume resume = new Resume();
        resume.setUser(currentUser);
        resume.setJob(currentJob);
        resume.setFileName("test-resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("test content".getBytes());

        Resume savedResume = resumeRepository.save(resume);

        Score score1 = new Score();
        score1.setUser(currentUser);
        score1.setJob(currentJob);
        score1.setResume(savedResume);
        score1.setOverallScore(BigDecimal.valueOf(85.5));
        score1.setMatchedKeywords("[\"Docker\",\"Kubernetes\"]");
        score1.setMissingKeywords("[\"AWS\"]");
        score1.setRecommendationsSummary("Strong match on core DevOps tools.");

        Score savedScore1 = scoreRepository.save(score1);

        Score score2 = new Score();
        score2.setUser(currentUser);
        score2.setJob(currentJob);
        score2.setResume(savedResume);
        score2.setOverallScore(BigDecimal.valueOf(80.5));
        score2.setMatchedKeywords("[\"Docker\",\"Kubernetes\", \"Java\"]");
        score2.setMissingKeywords("[\"AWS\", \"Python\"]");
        score2.setRecommendationsSummary("Strong match on core DevOps tools.");

        Score savedScore2 = scoreRepository.save(score2);

        entityManager.flush();
        entityManager.clear();

        List<Score> scores = scoreRepository.findByJobId(currentJob.getId());

        assertEquals(2, scores.size());
        assertTrue(scores.stream().anyMatch(s ->
                BigDecimal.valueOf(85.5).compareTo(s.getOverallScore()) == 0));
        assertTrue(scores.stream().anyMatch(s ->
                BigDecimal.valueOf(80.5).compareTo(s.getOverallScore()) == 0));
    }

    @Test
    void shouldFindByUserId(){

        ObjectMapper objectMapper = new ObjectMapper();

        User userA = new User();
        userA.setUsername("Risheek");
        userA.setEmail("risheekshrestha@gmail.com");
        userA.setPasswordHash("risheek@1234");
        userA.setRole(User.Role.USER);
        User savedUserA = userRepository.save(userA);

        User userB = new User();
        userB.setUsername("OtherUser");
        userB.setEmail("otheruser@gmail.com");
        userB.setPasswordHash("other@1234");
        userB.setRole(User.Role.USER);
        User savedUserB = userRepository.save(userB);

        Job job = new Job();

        job.setTitle("DevOps Engineer");
        job.setDescription("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(savedUserA);

        Job currentJob = jobRepository.save(job);

        JobSkill jobSkill1 = new JobSkill(null, currentJob, "Docker");
        JobSkill jobSkill2 = new JobSkill(null, currentJob, "Kubernetes");

        List<JobSkill> savedSkills = jobSkillRepository.saveAll(List.of(jobSkill1, jobSkill2));

        Resume resume = new Resume();
        resume.setUser(savedUserA);
        resume.setJob(currentJob);
        resume.setFileName("test-resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("test content".getBytes());

        Resume savedResume = resumeRepository.save(resume);

        Score score1 = new Score();
        score1.setUser(savedUserA);
        score1.setJob(currentJob);
        score1.setResume(savedResume);
        score1.setOverallScore(BigDecimal.valueOf(85.5));
        score1.setMatchedKeywords("[\"Docker\",\"Kubernetes\"]");
        score1.setMissingKeywords("[\"AWS\"]");
        score1.setRecommendationsSummary("Strong match on core DevOps tools.");

        Score savedScore1 = scoreRepository.save(score1);

        Score score2 = new Score();
        score2.setUser(savedUserA);
        score2.setJob(currentJob);
        score2.setResume(savedResume);
        score2.setOverallScore(BigDecimal.valueOf(80.5));
        score2.setMatchedKeywords("[\"Docker\",\"Kubernetes\", \"Java\"]");
        score2.setMissingKeywords("[\"AWS\", \"Python\"]");
        score2.setRecommendationsSummary("Strong match on core DevOps tools.");

        Score savedScore2 = scoreRepository.save(score2);

        Score score3 = new Score();
        score3.setUser(savedUserB);
        score3.setJob(currentJob);
        score3.setResume(savedResume);
        score3.setOverallScore(BigDecimal.valueOf(89.5));
        score3.setMatchedKeywords("[\"Kubernetes\", \"Java\"]");
        score3.setMissingKeywords("[\"Python\"]");
        score3.setRecommendationsSummary("Strong match on core DevOps tools.");

        Score savedScore = scoreRepository.save(score3);

        entityManager.flush();
        entityManager.clear();

        List<Score> scores = scoreRepository.findByUserId(savedUserA.getId());

        assertEquals(2, scores.size());
        assertTrue(scores.stream().anyMatch(s ->
                BigDecimal.valueOf(85.5).compareTo(s.getOverallScore()) == 0));
        assertTrue(scores.stream().anyMatch(s ->
                BigDecimal.valueOf(80.5).compareTo(s.getOverallScore()) == 0));

    }

}