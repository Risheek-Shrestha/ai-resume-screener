package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Course;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class CourseRepositoryTest {

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
    private CourseRepository courseRepository;

    @Test
    void shouldCreateAndFindCourse() {

        Course course = new Course();
        course.setName("MCA");
        course.setTotalYears(2);

        Course savedCourse = courseRepository.save(course);

        entityManager.flush();
        entityManager.clear();

        var found = courseRepository.findById(savedCourse.getId());

        assertTrue(found.isPresent());
        assertEquals("MCA", found.get().getName());
        assertEquals(2, found.get().getTotalYears());
    }

    @Test
    void shouldFindAllCourses() {

        Course course1 = new Course();
        course1.setName("B.Tech");
        course1.setTotalYears(4);

        Course course2 = new Course();
        course2.setName("MCA");
        course2.setTotalYears(2);

        courseRepository.save(course1);
        courseRepository.save(course2);

        entityManager.flush();
        entityManager.clear();

        var courses = courseRepository.findAll();

        assertEquals(2, courses.size());
    }

    @Test
    void shouldNotAllowDuplicateCourseName() {

        Course course1 = new Course();
        course1.setName("MCA");
        course1.setTotalYears(2);
        courseRepository.saveAndFlush(course1);

        Course course2 = new Course();
        course2.setName("MCA");
        course2.setTotalYears(3);

        assertThrows(DataIntegrityViolationException.class, () -> {
            courseRepository.saveAndFlush(course2);
        });
    }

    @Test
    void shouldDeleteCourse() {

        Course course = new Course();
        course.setName("MBA");
        course.setTotalYears(2);

        Course savedCourse = courseRepository.save(course);
        entityManager.flush();

        courseRepository.delete(savedCourse);
        entityManager.flush();
        entityManager.clear();

        var found = courseRepository.findById(savedCourse.getId());
        assertTrue(found.isEmpty());
    }
}