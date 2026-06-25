package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreRepository
        extends JpaRepository<Score, Long> {

    Optional<Score> findByApplicationId(Long applicationId);
}