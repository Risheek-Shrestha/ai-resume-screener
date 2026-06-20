package com.risheek.resume_screener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobSuggestionResponse {
    private Long jobId;
    private String jobTitle;
    private String jobType;
    private String experienceLevel;
    private double matchPercentage;
    private List<String> matchedSkills;
    private List<String> missingSkills;

    private double semanticSimilarity;
    private String whyMatch;
}