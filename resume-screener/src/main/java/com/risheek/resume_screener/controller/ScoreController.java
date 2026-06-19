package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.service.ScoreService;
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

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<ScoreResponse> getScoreByResume(
            @PathVariable Long resumeId) {

        return ResponseEntity.ok(
                scoreService.getScoreByResume(resumeId));
    }

    @GetMapping("/my-scores")
    public ResponseEntity<List<ScoreResponse>> getMyScores() {

        return ResponseEntity.ok(
                scoreService.getMyScores()
        );
    }
}
