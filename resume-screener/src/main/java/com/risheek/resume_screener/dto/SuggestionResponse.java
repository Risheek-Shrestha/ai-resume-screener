package com.risheek.resume_screener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionResponse {
    private Long resumeId;
    private BigDecimal currentScore;
    private String scoreLevel;
    private List<String> missingSkills;
    private List<String> weakAreas;
    private List<String> actionableSteps;
    private List<String> suggestedLearningPaths;
    private List<String> resumeImprovementTips;
}