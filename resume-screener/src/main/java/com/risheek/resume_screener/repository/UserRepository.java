package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Course;
import com.risheek.resume_screener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository <User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRole(User.Role role);
    boolean existsByCurrentCourse(Course course);
}
