package com.risheek.resume_screener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResponse {

    private Long id;

    private Long userId;

    private Long jobId;

    private Long resumeId;

    private BigDecimal overallScore;

    private String matchedKeywords;

    private String missingKeywords;

    private String recommendationsSummary;
}
