package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private Long applicationId;

    private Long jobId;

    private String jobTitle;

    private Long resumeId;

    private BigDecimal atsScore;

    private ApplicationStatus status;

    private LocalDateTime appliedAt;
}