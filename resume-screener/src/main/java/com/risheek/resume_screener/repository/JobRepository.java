package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByTitle(String title);
    boolean existsByTitle(String title);
}