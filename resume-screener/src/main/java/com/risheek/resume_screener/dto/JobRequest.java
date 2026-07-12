package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

    // Course IDs this job is restricted to. Leave empty/null for "open to all courses".
    private List<Long> eligibleCourseIds;

    // Semesters this job is restricted to. Leave empty/null for "open to all semesters".
    private Set<Integer> eligibleSemesters;
}