package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.EducationLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EducationResponse {

    private Long id;
    private Long userId;
    private EducationLevel level;
    private String institution;
    private LocalDate startDate;
    private LocalDate endDate;
    private String grade;
    private Boolean isCurrent;

}
