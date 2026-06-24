package com.risheek.resume_screener.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class JobPageResponse implements Serializable {
    private List<JobResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public JobPageResponse() {}

    public JobPageResponse(List<JobResponse> content, int page, int size,
                           long totalElements, int totalPages, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

}