package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.Job;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse implements Serializable {
    private Long id;
    private String title;
    private String description;
    private Job.JobType jobType;
    private Job.ExperienceLevel experienceLevel;
    private List<String> skills;
    private LocalDateTime createdAt;
}