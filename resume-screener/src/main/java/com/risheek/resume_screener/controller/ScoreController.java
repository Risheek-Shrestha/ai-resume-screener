package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.ScoreRequest;
import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.service.ScoreService;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<ScoreResponse> generateScore(
            @RequestBody @Valid ScoreRequest request) {

        return ResponseEntity.ok(
                scoreService.generateScore(
                        request.getResumeId(),
                        request.getJobId()
                )
        );
    }

    @GetMapping("/my-scores")
    public ResponseEntity<List<ScoreResponse>> getMyScores() {

        return ResponseEntity.ok(
                scoreService.getMyScores()
        );
    }
}
