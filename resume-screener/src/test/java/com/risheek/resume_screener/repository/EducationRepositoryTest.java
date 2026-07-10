package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Education;
import com.risheek.resume_screener.entity.EducationLevel;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class EducationRepositoryTest extends RepositoryTestHelper {

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
    private EducationRepository educationRepository;

    @Test
    void shouldCreateAndFindEducationByUser() {

        User user = createUser("Risheek", "risheek@example.com");

        Education education = new Education();
        education.setUser(user);
        education.setLevel(EducationLevel.MASTERS);
        education.setInstitution("Shoolini University");
        education.setStartDate(LocalDate.of(2025, 7, 1));
        education.setEndDate(null);
        education.setGrade("A");
        education.setIsCurrent(true);

        educationRepository.save(education);

        entityManager.flush();
        entityManager.clear();

        List<Education> found = educationRepository.findByUser(user);

        assertEquals(1, found.size());
        assertEquals("Shoolini University", found.getFirst().getInstitution());
        assertEquals(EducationLevel.MASTERS, found.getFirst().getLevel());
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoEducation() {

        User user = createUser("NoEdu", "noedu@example.com");

        entityManager.flush();
        entityManager.clear();

        List<Education> found = educationRepository.findByUser(user);

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldOnlyReturnEducationForGivenUser() {

        User user1 = createUser("UserOne", "user1@example.com");
        User user2 = createUser("UserTwo", "user2@example.com");

        Education education1 = new Education();
        education1.setUser(user1);
        education1.setLevel(EducationLevel.BACHELORS);
        education1.setInstitution("College A");
        education1.setStartDate(LocalDate.of(2020, 7, 1));
        education1.setEndDate(LocalDate.of(2024, 6, 30));
        education1.setGrade("B");
        education1.setIsCurrent(false);

        educationRepository.save(education1);

        Education education2 = new Education();
        education2.setUser(user2);
        education2.setLevel(EducationLevel.MASTERS);
        education2.setInstitution("College B");
        education2.setStartDate(LocalDate.of(2025, 7, 1));
        education2.setEndDate(null);
        education2.setGrade("A");
        education2.setIsCurrent(true);

        educationRepository.save(education2);

        entityManager.flush();
        entityManager.clear();

        List<Education> found = educationRepository.findByUser(user1);

        assertEquals(1, found.size());
        assertEquals("College A", found.getFirst().getInstitution());
    }
}