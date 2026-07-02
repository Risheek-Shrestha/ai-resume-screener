package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.service.ScoreService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/resume/{resumeId}/job/{jobId}")
    public ResponseEntity<ScoreResponse> getScoreByResume(
            @PathVariable Long resumeId, @PathVariable Long jobId) {
        return ResponseEntity.ok(scoreService.getScoreByResume(resumeId, jobId));
    }

    @GetMapping("/my-scores")
    public ResponseEntity<List<ScoreResponse>> getMyScores() {

        return ResponseEntity.ok(
                scoreService.getMyScores()
        );
    }
}
