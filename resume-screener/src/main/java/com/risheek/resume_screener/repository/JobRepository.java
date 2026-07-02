package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByTitle(String title);
    boolean existsByTitle(String title);
    @Query("SELECT j FROM Job j WHERE j.applicationStartsAt <= CURRENT_TIMESTAMP " +
            "AND j.applicationDeadline >= CURRENT_TIMESTAMP " +
            "AND j.id NOT IN (SELECT a.job.id FROM Application a WHERE a.user.id = :userId)")
    Page<Job> findOpenJobsNotAppliedByUser(@Param("userId") Long userId, Pageable pageable);
    Page<Job> findByUserId(Long userId, Pageable pageable);
}