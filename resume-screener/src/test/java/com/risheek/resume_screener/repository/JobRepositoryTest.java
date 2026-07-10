package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.*;
import com.risheek.resume_screener.specification.JobSpecification;
import com.risheek.resume_screener.util.RepositoryTestHelper;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class JobRepositoryTest extends RepositoryTestHelper {

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

    @Test
    void shouldCreateAndFindJob(){

        User currentUser = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        Job job = new Job();

        job.setTitle("DevOps Engineer");
        job.setDescription("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(currentUser);
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        Job currentJob = jobRepository.save(job);

        entityManager.flush();
        entityManager.clear();

        var found = jobRepository.findById(currentJob.getId());

        assertTrue(found.isPresent());
        assertEquals("DevOps Engineer", found.get().getTitle());
        assertEquals("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.", found.get().getDescription());
        assertEquals(Job.JobType.FULL_TIME, found.get().getJobType());
        assertEquals(Job.ExperienceLevel.MID, found.get().getExperienceLevel());
    }

    @Test
    void shouldFindByTitle(){

        User currentUser = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        Job job = new Job();

        job.setTitle("DevOps Engineer");
        job.setDescription("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(currentUser);
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        Job currentJob = jobRepository.save(job);

        entityManager.flush();
        entityManager.clear();

        var found = jobRepository.findByTitle(currentJob.getTitle());

        assertTrue(found.isPresent());
        assertEquals("DevOps Engineer", found.get().getTitle());
        assertEquals("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.", found.get().getDescription());
        assertEquals(Job.JobType.FULL_TIME, found.get().getJobType());
        assertEquals(Job.ExperienceLevel.MID, found.get().getExperienceLevel());
    }

    @Test
    void shouldExistsByTitle(){

        User currentUser = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        Job job = new Job();

        job.setTitle("DevOps Engineer");
        job.setDescription("We are looking for a DevOps Engineer with experience in Linux, Docker, Kubernetes, AWS, CI/CD pipelines, Terraform, and monitoring tools. The candidate should automate deployments, manage cloud infrastructure, and improve system reliability.");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(currentUser);
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        Job currentJob = jobRepository.save(job);

        entityManager.flush();
        entityManager.clear();

        Boolean found = jobRepository.existsByTitle(currentJob.getTitle());

        assertTrue(found);

    }

    @Test
    void shouldFindOpenJobsNotAppliedByUser() {

        User user = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        Job job = new Job();
        job.setTitle("Backend Developer");
        job.setDescription("Spring Boot");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(user);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(5));

        jobRepository.save(job);

        entityManager.flush();
        entityManager.clear();

        Specification<Job> spec = JobSpecification.isOpenNow()
                .and(JobSpecification.notAppliedByUser(user.getId()));

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Backend Developer",
                result.getContent().getFirst().getTitle());
    }

    @Test
    void shouldNotReturnClosedJobs() {

        User user = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        Job job = new Job();
        job.setTitle("Closed Job");
        job.setDescription("Closed");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(user);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(10));
        job.setApplicationDeadline(LocalDateTime.now().minusDays(1));

        jobRepository.save(job);

        entityManager.flush();
        entityManager.clear();

        Specification<Job> spec = JobSpecification.isOpenNow()
                .and(JobSpecification.notAppliedByUser(user.getId()));

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotReturnJobsNotStartedYet() {

        User user = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        Job job = new Job();
        job.setTitle("Future Job");
        job.setDescription("Future");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(user);
        job.setApplicationStartsAt(LocalDateTime.now().plusDays(2));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(10));

        jobRepository.save(job);

        entityManager.flush();
        entityManager.clear();

        Specification<Job> spec = JobSpecification.isOpenNow()
                .and(JobSpecification.notAppliedByUser(user.getId()));

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldExcludeAppliedJobs() {

        User user = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        Job job = new Job();
        job.setTitle("Backend");
        job.setDescription("Spring");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(user);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(5));

        job = jobRepository.save(job);

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setResumeName("Resume");
        resume.setFileName("resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("test".getBytes());
        resume.setActive(true);

        entityManager.persist(resume);

        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setResume(resume);
        application.setStatus(ApplicationStatus.APPLIED);

        entityManager.persist(application);
        entityManager.flush();
        entityManager.clear();

        Specification<Job> spec = JobSpecification.isOpenNow()
                .and(JobSpecification.notAppliedByUser(user.getId()));

        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }
}