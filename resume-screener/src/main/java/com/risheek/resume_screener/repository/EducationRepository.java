package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Education;
import com.risheek.resume_screener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findByUser(User user);
}