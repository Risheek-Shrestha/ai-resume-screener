package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeRepository extends JpaRepository <Resume, Long> {
    List<Resume> findByUserId(Long userId);
}
