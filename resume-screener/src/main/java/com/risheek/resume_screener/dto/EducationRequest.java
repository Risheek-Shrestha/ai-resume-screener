package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.EducationLevel;
import com.risheek.resume_screener.validation.ValidEducationDates;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@ValidEducationDates
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EducationRequest {

    @NotNull(message = "Enter education level")
    private EducationLevel level;

    @NotBlank(message = "Enter institution name")
    private String institution;

    @NotNull(message = "Enter start date")
    private LocalDate startDate;
    
    private LocalDate endDate;

    @NotBlank(message = "Enter grade")
    private String grade;

    @NotNull(message = "Enter passing status")
    private Boolean isCurrent;

}
