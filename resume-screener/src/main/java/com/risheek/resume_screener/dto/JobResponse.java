package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.ApplicationStatus;
import com.risheek.resume_screener.entity.ApplicationWindowStatus;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.service.JobService;
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
    private LocalDateTime applicationStartsAt;
    private LocalDateTime applicationDeadline;
    private ApplicationWindowStatus applicationStatus;

    // The current (most recent) authenticated user's application status for
    // this job - APPLIED / SHORTLISTED / HIRED / REJECTED - or null if the
    // user has never applied. Only populated for user-specific endpoints
    // (e.g. GET /jobs/open); left null on globally-cached responses.
    private ApplicationStatus userApplicationStatus;
}