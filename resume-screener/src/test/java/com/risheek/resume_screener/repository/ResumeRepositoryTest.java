package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.User;
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
class ResumeRepositoryTest {

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
    private ResumeRepository resumeRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveandRetrieveResume(){

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
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        Job currentJob = jobRepository.save(job);

        Resume resume = new Resume();
        resume.setUser(currentUser);
        resume.setJob(currentJob);
        resume.setFileName("test-resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("test content".getBytes());
        resume.setResumeName("Backend Developer Resume");

        Resume savedResume = resumeRepository.save(resume);
        entityManager.flush();
        entityManager.clear();

        var found = resumeRepository.findById(savedResume.getId());

        assertTrue(found.isPresent());
        assertEquals("test-resume.pdf", found.get().getFileName());
        assertEquals("application/pdf", found.get().getFileType());
        assertArrayEquals("test content".getBytes(), found.get().getFileData());
        assertEquals(currentUser.getId(), found.get().getUser().getId());
        assertEquals(currentJob.getId(), found.get().getJob().getId());
    }

    @Test
    void shouldFindResumeByUserId(){
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
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        Job currentJob = jobRepository.save(job);

        Resume resume1 = new Resume();
        resume1.setUser(currentUser);
        resume1.setJob(currentJob);
        resume1.setFileName("test-resume.pdf");
        resume1.setFileType("application/pdf");
        resume1.setFileData("test content".getBytes());
        resume1.setResumeName("Backend Developer Resume");

        Resume savedResume1 = resumeRepository.save(resume1);

        Resume resume2 = new Resume();
        resume2.setUser(currentUser);
        resume2.setJob(currentJob);
        resume2.setFileName("test-resume2.pdf");
        resume2.setFileType("application2/pdf");
        resume2.setFileData("test content".getBytes());
        resume2.setResumeName("Backend Developer Resume");

        Resume savedResume2 = resumeRepository.save(resume2);

        entityManager.flush();
        entityManager.clear();

        List<Resume> resumes = resumeRepository.findByUserId(currentUser.getId());

        assertEquals(2, resumes.size());
        assertTrue(resumes.stream().anyMatch(r -> r.getFileName().equals("test-resume.pdf")));
        assertTrue(resumes.stream().anyMatch(r -> r.getFileName().equals("test-resume2.pdf")));
    }

    @Test
    void shouldReturnEmptyWhenResumeBelongsToDifferentUser() {
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
        job.setDescription("...");
        job.setJobType(Job.JobType.FULL_TIME);
        job.setExperienceLevel(Job.ExperienceLevel.MID);
        job.setUser(savedUserA);
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));
        Job savedJob = jobRepository.save(job);

        Resume resume = new Resume();
        resume.setUser(savedUserA);
        resume.setJob(savedJob);
        resume.setFileName("test-resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData("test content".getBytes());
        resume.setResumeName("Backend Developer Resume");
        Resume savedResume = resumeRepository.save(resume);

        entityManager.flush();
        entityManager.clear();

        var found = resumeRepository.findByIdAndUserId(savedResume.getId(), savedUserB.getId());

        assertTrue(found.isEmpty());
    }

}