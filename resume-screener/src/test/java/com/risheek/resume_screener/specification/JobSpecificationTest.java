package com.risheek.resume_screener.specification;

import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.JobSkillRepository;
import com.risheek.resume_screener.util.RepositoryTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class JobSpecificationTest extends RepositoryTestHelper {

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
    private TestEntityManager entityManager;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    private User user;
    private Job backendJob;
    private Job internshipJob;

    @BeforeEach
    void setUp() {
        user = createUser(
                "Risheek",
                "risheek@gmail.com"
        );

        backendJob = new Job();
        backendJob.setTitle("Senior Backend Engineer");
        backendJob.setDescription("Spring Boot role");
        backendJob.setJobType(Job.JobType.FULL_TIME);
        backendJob.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        backendJob.setUser(user);
        backendJob.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        backendJob.setApplicationDeadline(LocalDateTime.now().plusDays(5));
        backendJob = jobRepository.save(backendJob);

        internshipJob = new Job();
        internshipJob.setTitle("Frontend Intern");
        internshipJob.setDescription("React role");
        internshipJob.setJobType(Job.JobType.INTERNSHIP);
        internshipJob.setExperienceLevel(Job.ExperienceLevel.JUNIOR);
        internshipJob.setUser(user);
        internshipJob.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        internshipJob.setApplicationDeadline(LocalDateTime.now().plusDays(5));
        internshipJob = jobRepository.save(internshipJob);

        jobSkillRepository.save(new JobSkill(null, backendJob, "Java"));
        jobSkillRepository.save(new JobSkill(null, backendJob, "Spring Boot"));
        jobSkillRepository.save(new JobSkill(null, internshipJob, "React"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void hasKeyword_matchesPartialCaseInsensitiveTitle() {
        Specification<Job> spec = JobSpecification.hasKeyword("backend");

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Senior Backend Engineer", result.getContent().getFirst().getTitle());
    }

    @Test
    void hasKeyword_doesNotMatchUnrelatedTitle() {
        Specification<Job> spec = JobSpecification.hasKeyword("devops");

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void hasJobType_matchesExactType() {
        Specification<Job> spec = JobSpecification.hasJobType(Job.JobType.INTERNSHIP);

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Frontend Intern", result.getContent().getFirst().getTitle());
    }

    @Test
    void hasJobType_excludesOtherTypes() {
        Specification<Job> spec = JobSpecification.hasJobType(Job.JobType.PART_TIME);

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void hasExperienceLevel_matchesExactLevel() {
        Specification<Job> spec = JobSpecification.hasExperienceLevel(Job.ExperienceLevel.SENIOR);

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Senior Backend Engineer", result.getContent().getFirst().getTitle());
    }

    @Test
    void hasExperienceLevel_excludesOtherLevels() {
        Specification<Job> spec = JobSpecification.hasExperienceLevel(Job.ExperienceLevel.MID);

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void hasSkill_matchesJobWithSkill() {
        Specification<Job> spec = JobSpecification.hasSkill("java");

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Senior Backend Engineer", result.getContent().getFirst().getTitle());
    }

    @Test
    void hasSkill_isCaseInsensitiveAndSupportsPartialMatch() {
        Specification<Job> spec = JobSpecification.hasSkill("SPRING");

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Senior Backend Engineer", result.getContent().getFirst().getTitle());
    }

    @Test
    void hasSkill_excludesJobsWithoutSkill() {
        Specification<Job> spec = JobSpecification.hasSkill("Python");

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void buildSpecification_allFiltersNull_returnsEverything() {
        Specification<Job> spec = JobSpecification.buildSpecification(null, null, null, null);

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void buildSpecification_singleFilter_appliesOnlyThatFilter() {
        Specification<Job> spec = JobSpecification.buildSpecification(
                null, Job.JobType.FULL_TIME, null, null);

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Senior Backend Engineer", result.getContent().getFirst().getTitle());
    }

    @Test
    void buildSpecification_multipleFilters_combineWithAnd() {
        Specification<Job> spec = JobSpecification.buildSpecification(
                "backend", Job.JobType.FULL_TIME, Job.ExperienceLevel.SENIOR, "Java");

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Senior Backend Engineer", result.getContent().getFirst().getTitle());
    }

    @Test
    void buildSpecification_contradictoryFilters_returnsNothing() {
        Specification<Job> spec = JobSpecification.buildSpecification(
                "backend", Job.JobType.INTERNSHIP, null, null);

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void buildSpecification_blankKeywordAndSkill_areIgnored() {
        Specification<Job> spec = JobSpecification.buildSpecification(
                "  ", Job.JobType.FULL_TIME, null, "   ");

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Senior Backend Engineer", result.getContent().getFirst().getTitle());
    }

    @Test
    void isOpenNow_excludesClosedAndNotYetStartedJobs() {
        Job closedJob = new Job();
        closedJob.setTitle("Closed Role");
        closedJob.setDescription("Closed");
        closedJob.setJobType(Job.JobType.FULL_TIME);
        closedJob.setExperienceLevel(Job.ExperienceLevel.MID);
        closedJob.setUser(user);
        closedJob.setApplicationStartsAt(LocalDateTime.now().minusDays(10));
        closedJob.setApplicationDeadline(LocalDateTime.now().minusDays(1));
        jobRepository.save(closedJob);

        entityManager.flush();
        entityManager.clear();

        Specification<Job> spec = JobSpecification.isOpenNow();

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        List<String> titles = result.getContent().stream().map(Job::getTitle).toList();
        assertTrue(titles.containsAll(List.of("Senior Backend Engineer", "Frontend Intern")));
        assertFalse(titles.contains("Closed Role"));
    }

    @Test
    void notAppliedByUser_returnsAllJobsWhenUserHasNoApplications() {
        Specification<Job> spec = JobSpecification.notAppliedByUser(user.getId());

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }
}