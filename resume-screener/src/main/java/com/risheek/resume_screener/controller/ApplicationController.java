package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.ApplicationRequest;
import com.risheek.resume_screener.dto.ApplicationResponse;
import com.risheek.resume_screener.dto.ApplicationResultResponse;
import com.risheek.resume_screener.dto.SuggestionResponse;
import com.risheek.resume_screener.entity.Application;
import com.risheek.resume_screener.entity.ApplicationStatus;
import com.risheek.resume_screener.service.ApplicationService;
import com.risheek.resume_screener.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final SuggestionService suggestionService;

    public ApplicationController(ApplicationService applicationService, SuggestionService suggestionService) {
        this.applicationService = applicationService;
        this.suggestionService = suggestionService;
    }

    @PostMapping("/jobs/{jobId}")
    public ResponseEntity<ApplicationResultResponse> applyForJob(
            @PathVariable Long jobId, @Valid @RequestBody ApplicationRequest request) {

        Application application = applicationService.applyForJob(jobId, request);
        SuggestionResponse suggestions =
                suggestionService.getImprovementSuggestions(request.getResumeId());

        String message = resolveMessage(application.getStatus(), application.getScore().getOverallScore());

        return ResponseEntity.status(201).body(new ApplicationResultResponse(
                application.getId(),
                application.getJob().getId(),
                application.getStatus(),
                message,
                application.getScore().getOverallScore(),
                suggestions
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications() {
        return ResponseEntity.ok(applicationService.getMyApplications());
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForJob(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(
                applicationService.getApplicationsForJob(jobId));
    }

    @GetMapping("/jobs/{jobId}/accepted")
    public ResponseEntity<List<ApplicationResponse>> getAcceptedApplicationsForJob(
            @PathVariable Long jobId) {

        return ResponseEntity.ok(
                applicationService.getAcceptedApplicationsForJob(jobId));
    }

    private String resolveMessage(ApplicationStatus status, BigDecimal score) {
        return switch (status) {
            case APPLIED -> "Your profile matches this role — application submitted.";
            case REJECTED -> "Your score didn't meet the threshold for this role. Check your suggestions below to improve.";
            default -> "Application processed.";
        };
    }
}