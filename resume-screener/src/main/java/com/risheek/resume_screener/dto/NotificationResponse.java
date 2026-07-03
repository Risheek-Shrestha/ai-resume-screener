package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Long jobId;
    private Long applicationId;
    private boolean read;
    private LocalDateTime createdAt;
}
