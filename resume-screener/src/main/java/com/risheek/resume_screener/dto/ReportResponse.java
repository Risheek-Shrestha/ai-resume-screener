package com.risheek.resume_screener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ReportResponse {

    private Long resumeId;
    private String jobTitle;

    private BigDecimal overallScore;
    private String scoreLevel;

    private List<String> strengths;
    private List<String> gaps;
    private List<String> improvements;

    private String jobReadiness;
}