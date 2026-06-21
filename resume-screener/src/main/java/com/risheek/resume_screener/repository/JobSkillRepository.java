package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {
    List<JobSkill> findByJobId(Long jobId);
    void deleteByJobId(Long jobId);
}