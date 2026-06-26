package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreRepository
        extends JpaRepository<Score, Long> {

    Optional<Score> findByResumeId(Long resumeId);

    List<Score> findByJobId(Long jobId);

    List<Score> findByUserId(Long userId);
}