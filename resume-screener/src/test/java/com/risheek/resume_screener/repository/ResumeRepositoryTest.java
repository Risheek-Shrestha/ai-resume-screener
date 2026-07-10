package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Resume;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class ResumeRepositoryTest extends RepositoryTestHelper {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private ResumeRepository resumeRepository;
    @Autowired private TestEntityManager entityManager;

    private User savedUser() {
        return createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );
    }

    @Test
    void shouldSaveAndRetrieveResume() {
        User currentUser = savedUser();

        Resume resume = new Resume();
        resume.setUser(currentUser);
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
    }

    @Test
    void shouldFindResumeByUserId() {
        User currentUser = savedUser();

        Resume resume1 = new Resume();
        resume1.setUser(currentUser);
        resume1.setFileName("test-resume.pdf");
        resume1.setFileType("application/pdf");
        resume1.setFileData("test content".getBytes());
        resume1.setResumeName("Backend Developer Resume");
        resumeRepository.save(resume1);

        Resume resume2 = new Resume();
        resume2.setUser(currentUser);
        resume2.setFileName("test-resume2.pdf");
        resume2.setFileType("application/pdf");
        resume2.setFileData("test content".getBytes());
        resume2.setResumeName("Backend Developer Resume 2");
        resumeRepository.save(resume2);

        entityManager.flush();
        entityManager.clear();

        List<Resume> resumes = resumeRepository.findByUserId(currentUser.getId());

        assertEquals(2, resumes.size());
        assertTrue(resumes.stream().anyMatch(r -> r.getFileName().equals("test-resume.pdf")));
        assertTrue(resumes.stream().anyMatch(r -> r.getFileName().equals("test-resume2.pdf")));
    }

    @Test
    void shouldReturnEmptyWhenResumeBelongsToDifferentUser() {
        User savedUserA = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        User savedUserB = createUser(
                "OtherUser",
                "otheruser@gmail.com"
        );

        Resume resume = new Resume();
        resume.setUser(savedUserA);
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
