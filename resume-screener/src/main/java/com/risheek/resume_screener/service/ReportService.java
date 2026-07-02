package com.risheek.resume_screener.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.dto.ReportResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.Score;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.ScoreRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final ResumeRepository resumeRepository;
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public ReportService(
            ResumeRepository resumeRepository,
            ScoreRepository scoreRepository,
            UserRepository userRepository,
            JobRepository jobRepository,
            ObjectMapper objectMapper
    ) {
        this.resumeRepository = resumeRepository;
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    public ReportResponse generateReport(Long resumeId, Long jobId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found"));

        if (!resume.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to view this report");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        Score score = scoreRepository.findByResumeIdAndJobId(resumeId, jobId)
                .orElseThrow(() -> new ScoreNotFoundException("Score not found for resume"));

        List<String> matchedSkills = parseJsonList(score.getMatchedKeywords());
        List<String> missingSkills = parseJsonList(score.getMissingKeywords());

        List<String> improvements = new ArrayList<>(
                missingSkills.stream()
                        .map(skill -> "Add experience with " + skill)
                        .toList()
        );

        if (improvements.isEmpty()) {
            improvements = List.of(
                    "Continue building real-world projects",
                    "Quantify project achievements",
                    "Tailor resume to each application"
            );
        }

        double overallScore = score.getOverallScore().doubleValue();

        return new ReportResponse(
                resume.getId(),
                job.getTitle(),
                score.getOverallScore(),
                getScoreLevel(overallScore),
                matchedSkills,
                missingSkills,
                improvements,
                getJobReadiness(overallScore)
        );
    }

    private String getScoreLevel(double score) {
        if (score >= 80) return "EXCELLENT";
        if (score >= 60) return "GOOD";
        if (score >= 40) return "MODERATE";
        return "WEAK";
    }

    private String getJobReadiness(double score) {
        if (score >= 80) return "Interview Ready";
        if (score >= 60) return "Potential Candidate";
        if (score >= 40) return "Needs Improvement";
        return "Not Ready";
    }

    private List<String> parseJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
