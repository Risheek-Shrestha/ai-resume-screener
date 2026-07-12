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
import java.util.Set;

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

    // Courses/semesters this job is restricted to. Empty lists mean "open to everyone".
    private List<CourseResponse> eligibleCourses;
    private Set<Integer> eligibleSemesters;

    // Whether the current authenticated user meets this job's course/semester
    // restrictions. Only populated for user-specific endpoints (e.g. GET
    // /jobs/open); left null on globally-cached responses.
    private Boolean eligibleForCurrentUser;

    // Backward-compatible constructor for call sites (mainly existing tests)
    // predating the userApplicationStatus/eligibility fields. Defaults them.
    public JobResponse(Long id, String title, String description, Job.JobType jobType,
                        Job.ExperienceLevel experienceLevel, List<String> skills, LocalDateTime createdAt,
                        LocalDateTime applicationStartsAt, LocalDateTime applicationDeadline,
                        ApplicationWindowStatus applicationStatus) {
        this(id, title, description, jobType, experienceLevel, skills, createdAt,
                applicationStartsAt, applicationDeadline, applicationStatus, null,
                List.of(), Set.of(), null);
    }
}
