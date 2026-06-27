package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationNotificationEvent {

    private String email;
    private String jobTitle;
    private ApplicationStatus status;

}
