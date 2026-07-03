package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    Optional<Job> findByTitle(String title);
    boolean existsByTitle(String title);
    Page<Job> findByUserId(Long userId, Pageable pageable);
}