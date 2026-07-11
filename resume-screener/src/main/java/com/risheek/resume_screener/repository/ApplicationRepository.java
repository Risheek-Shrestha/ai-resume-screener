package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Application;
import com.risheek.resume_screener.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    // Used to block re-applying while a previous application for the same
    // job is still "active" (i.e. not REJECTED). Once REJECTED, this returns
    // false and the user is free to apply again.
    boolean existsByUserIdAndJobIdAndStatusNot(Long userId, Long jobId, ApplicationStatus status);

    List<Application> findByUserId(Long userId);

    List<Application> findByJobId(Long jobId);

    List<Application> findByJobIdAndStatusOrderByScoreOverallScoreDesc(
            Long jobId,
            ApplicationStatus status
    );

    List<Application> findByStatus(ApplicationStatus status);

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    // Latest application a user has submitted for a given job, used to
    // surface the user's current status (Applied / Shortlisted / Hired /
    // Rejected) on the browse-jobs listing.
    Optional<Application> findFirstByUserIdAndJobIdOrderByAppliedAtDesc(Long userId, Long jobId);
}