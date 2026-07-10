package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.entity.User;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class JobSkillRepositoryTest extends RepositoryTestHelper {

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

    @Test
    void shouldFindSkillsByJobId(){

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

        JobSkill jobSkill1 = new JobSkill(null, currentJob, "Docker");
        JobSkill jobSkill2 = new JobSkill(null, currentJob, "Kubernetes");

        jobSkillRepository.saveAll(List.of(jobSkill1, jobSkill2));

        entityManager.flush();
        entityManager.clear();

        List<JobSkill> jobSkills =  jobSkillRepository.findByJobId(currentJob.getId());
        
        assertEquals(2, jobSkills.size());
        assertTrue(jobSkills.stream().anyMatch(r -> r.getSkillName().equals("Docker")));
        assertTrue(jobSkills.stream().anyMatch(r -> r.getSkillName().equals("Kubernetes")));

    }

    @Test
    void shouldDeleteSkillsByJobId(){

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

        JobSkill jobSkill1 = new JobSkill(null, currentJob, "Docker");
        JobSkill jobSkill2 = new JobSkill(null, currentJob, "Kubernetes");

        jobSkillRepository.saveAll(List.of(jobSkill1, jobSkill2));

        entityManager.flush();
        entityManager.clear();

        jobSkillRepository.deleteByJobId(currentJob.getId());

        entityManager.flush();
        entityManager.clear();

        List<JobSkill> remaining = jobSkillRepository.findByJobId(currentJob.getId());

        assertTrue(remaining.isEmpty());
    }

}