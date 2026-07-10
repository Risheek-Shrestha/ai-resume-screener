package com.risheek.resume_screener.util;

import com.risheek.resume_screener.entity.Course;
import com.risheek.resume_screener.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.UUID;

public abstract class RepositoryTestHelper {

    @Autowired
    protected TestEntityManager entityManager;

    protected Course createCourse() {
        Course course = new Course();
        course.setName("Course-" + UUID.randomUUID());
        course.setTotalYears(2);

        return entityManager.persistAndFlush(course);
    }

    protected User createUser(String username, String email) {
        Course course = createCourse();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("password");
        user.setRole(User.Role.USER);

        user.setPhoneNumber("9876543210");
        user.setDateOfBirth(LocalDate.of(2003, 1, 1));
        user.setCurrentCollege("Shoolini University");
        user.setCurrentCourse(course);
        user.setCurrentSemester(2);

        return entityManager.persistAndFlush(user);
    }
}