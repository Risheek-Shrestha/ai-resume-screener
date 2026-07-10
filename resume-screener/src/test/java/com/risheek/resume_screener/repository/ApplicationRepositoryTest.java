package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.*;
import com.risheek.resume_screener.util.RepositoryTestHelper;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class ApplicationRepositoryTest extends RepositoryTestHelper {

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
    private ApplicationRepository applicationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldExistsByUserIdAndJobId() {

        Application application = createApplication(
                "user@test.com",
                "Backend Developer",
                BigDecimal.valueOf(80),
                ApplicationStatus.APPLIED);

        assertTrue(applicationRepository.existsByUserIdAndJobId(
                application.getUser().getId(),
                application.getJob().getId()));
    }

    @Test
    void shouldNotExistByUserIdAndJobId_whenNoApplication() {
        assertFalse(applicationRepository.existsByUserIdAndJobId(999L, 999L));
    }

    @Test
    void shouldFindByUserId() {

        Application application = createApplication(
                "user@test.com",
                "Backend Developer",
                BigDecimal.valueOf(80),
                ApplicationStatus.APPLIED);

        List<Application> result =
                applicationRepository.findByUserId(application.getUser().getId());

        assertEquals(1, result.size());
        assertEquals(ApplicationStatus.APPLIED,
                result.getFirst().getStatus());
    }

    @Test
    void shouldFindByJobId() {

        Application application = createApplication(
                "user@test.com",
                "Backend Developer",
                BigDecimal.valueOf(80),
                ApplicationStatus.APPLIED);

        List<Application> result =
                applicationRepository.findByJobId(application.getJob().getId());

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindByStatus() {

        createApplication(
                "user@test.com",
                "Backend Developer",
                BigDecimal.valueOf(80),
                ApplicationStatus.APPLIED);

        List<Application> result =
                applicationRepository.findByStatus(ApplicationStatus.APPLIED);

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindByIdAndUserId() {

        Application application = createApplication(
                "user@test.com",
                "Backend Developer",
                BigDecimal.valueOf(80),
                ApplicationStatus.APPLIED);

        var result = applicationRepository.findByIdAndUserId(
                application.getId(),
                application.getUser().getId());

        assertTrue(result.isPresent());
    }

    @Test
    void shouldNotFindByIdAndUserId_whenUserMismatch() {

        Application application = createApplication(
                "owner@test.com",
                "Backend Developer",
                BigDecimal.valueOf(80),
                ApplicationStatus.APPLIED);

        var result = applicationRepository.findByIdAndUserId(
                application.getId(),
                999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldOrderByScoreDescending() {

        Application low = createApplication(
                "user1@test.com",
                "Java Developer",
                BigDecimal.valueOf(65),
                ApplicationStatus.APPLIED);

        Application high = createApplicationForExistingJob(
                "user2@test.com",
                low.getJob(),
                BigDecimal.valueOf(95),
                ApplicationStatus.APPLIED);

        List<Application> result =
                applicationRepository.findByJobIdAndStatusOrderByScoreOverallScoreDesc(
                        low.getJob().getId(), ApplicationStatus.APPLIED);

        assertEquals(2, result.size());
        assertEquals(BigDecimal.valueOf(95),
                result.get(0).getScore().getOverallScore());
        assertEquals(BigDecimal.valueOf(65),
                result.get(1).getScore().getOverallScore());
    }

    private Application createApplication(String email,
                                          String title,
                                          BigDecimal scoreValue,
                                          ApplicationStatus status) {

        User user = createUser("user", email);

        Job job = new Job();
        job.setTitle(title);
        job.setDescription("Description");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(user);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(5));
        entityManager.persist(job);

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setResumeName("Resume");
        resume.setFileName("resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("abc".getBytes());
        resume.setActive(true);
        entityManager.persist(resume);

        Score score = new Score();
        score.setUser(user);
        score.setJob(job);
        score.setResume(resume);
        score.setOverallScore(scoreValue);
        score.setMatchedKeywords(List.of().toString());
        score.setMissingKeywords(List.of().toString());
        entityManager.persist(score);

        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setResume(resume);
        application.setScore(score);
        application.setStatus(status);

        entityManager.persist(application);
        entityManager.flush();

        return application;
    }

    private Application createApplicationForExistingJob(String email,
                                                        Job job,
                                                        BigDecimal scoreValue,
                                                        ApplicationStatus status) {

        User user = createUser("user", email);

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setResumeName("Resume");
        resume.setFileName("resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("abc".getBytes());
        resume.setActive(true);
        entityManager.persist(resume);

        Score score = new Score();
        score.setUser(user);
        score.setJob(job);
        score.setResume(resume);
        score.setOverallScore(scoreValue);
        score.setMatchedKeywords(List.of().toString());
        score.setMissingKeywords(List.of().toString());
        entityManager.persist(score);

        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setResume(resume);
        application.setScore(score);
        application.setStatus(status);

        entityManager.persist(application);
        entityManager.flush();

        return application;
    }
}