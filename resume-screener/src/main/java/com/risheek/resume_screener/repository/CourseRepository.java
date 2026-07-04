package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
