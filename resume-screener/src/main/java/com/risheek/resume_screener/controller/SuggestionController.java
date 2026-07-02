package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.JobSuggestionResponse;
import com.risheek.resume_screener.dto.SuggestionResponse;
import com.risheek.resume_screener.service.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/improve/{resumeId}/job/{jobId}")
    public ResponseEntity<SuggestionResponse> getImprovementSuggestions(
            @PathVariable Long resumeId, @PathVariable Long jobId) {
        return ResponseEntity.ok(
                suggestionService.getImprovementSuggestions(resumeId, jobId));
    }

    @GetMapping("/jobs/{resumeId}/job/{jobId}")
    public ResponseEntity<List<JobSuggestionResponse>> getSuggestedJobs(
            @PathVariable Long resumeId , @PathVariable Long jobId) {
        return ResponseEntity.ok(
                suggestionService.getSuggestedJobs(resumeId, jobId));
    }
}