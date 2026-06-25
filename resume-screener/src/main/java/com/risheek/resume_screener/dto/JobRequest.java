package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobRequest {

    @NotBlank(message = "Job title required")
    private String title;

    @NotBlank(message = "Job description required")
    private String description;

    @NotNull(message = "Please mention job type")
    private Job.JobType jobType;

    @NotNull(message = "Enter experience level")
    private Job.ExperienceLevel experienceLevel;

    private List<String> skills;

    @NotNull(message = "Enter application start date and time")
    private LocalDateTime applicationStartsAt;

    @NotNull(message = "Enter application deadline date and time")
    private LocalDateTime applicationDeadline;
}