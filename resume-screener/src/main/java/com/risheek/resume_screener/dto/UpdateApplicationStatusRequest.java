package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateApplicationStatusRequest {

    @NotNull
    private ApplicationStatus status;
}