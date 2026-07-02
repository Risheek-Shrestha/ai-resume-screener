package com.risheek.resume_screener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeResponse {

    private Long id;

    private String resumeName;

    private String fileName;

    private String fileType;
}
