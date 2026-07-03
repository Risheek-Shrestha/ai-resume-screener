package com.risheek.resume_screener.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class NotificationPageResponse implements Serializable {
    private List<NotificationResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public NotificationPageResponse() {}

    public NotificationPageResponse(List<NotificationResponse> content, int page, int size,
                                     long totalElements, int totalPages, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }
}
