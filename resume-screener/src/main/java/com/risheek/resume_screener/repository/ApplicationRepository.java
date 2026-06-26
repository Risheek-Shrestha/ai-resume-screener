package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Application;
import com.risheek.resume_screener.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    List<Application> findByUserId(Long userId);

    List<Application> findByJobId(Long jobId);

    List<Application> findByJobIdAndStatusOrderByScoreOverallScoreDesc(
            Long jobId,
            ApplicationStatus status
    );

    List<Application> findByStatus(ApplicationStatus status);

    Optional<Application> findByIdAndUserId(Long id, Long userId);
}