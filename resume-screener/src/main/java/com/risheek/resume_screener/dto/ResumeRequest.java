package com.risheek.resume_screener.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResumeRequest {

    @NotBlank(message = "File name required")
    private String fileName;

    @NotBlank(message = "File type required")
    private String fileType;

    @NotNull(message = "File data required")
    private byte[] fileData;

    private Long jobId;

}
