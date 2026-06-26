package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResultResponse {
    private Long applicationId;
    private Long jobId;
    private ApplicationStatus status;
    private String message;
    private BigDecimal score;
    private SuggestionResponse suggestions;
}