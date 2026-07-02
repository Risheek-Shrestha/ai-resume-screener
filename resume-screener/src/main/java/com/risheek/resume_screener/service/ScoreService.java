package com.risheek.resume_screener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.entity.*;
import com.risheek.resume_screener.exception.*;
import com.risheek.resume_screener.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final JobSkillRepository jobSkillRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;

    public ScoreService(ScoreRepository scoreRepository,
                        UserRepository userRepository,
                        JobSkillRepository jobSkillRepository,
                        WebClient.Builder webClientBuilder,
                        ObjectMapper objectMapper,
                        ResumeRepository resumeRepository,
                        JobRepository jobRepository,
                        @Value("${ml.service.url}") String mlServiceUrl) {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.webClient = webClientBuilder.baseUrl(mlServiceUrl).build();
        this.objectMapper = objectMapper;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Score generateScore(Resume resume, Job job) {
        List<String> jobSkills = jobSkillRepository
                .findByJobId(job.getId())
                .stream()
                .map(JobSkill::getSkillName)
                .toList();

        Map<String, Object> mlRequest = Map.of(
                "resumeText", resume.getParsedText(),
                "jobSkills", jobSkills,
                "jobDescription", job.getDescription()
        );

        Score score = scoreRepository.findByResumeIdAndJobId(resume.getId(), job.getId())
                .orElseGet(() -> buildNewScore(resume, job));

        score.setUser(resume.getUser());
        score.setJob(job);

        try {
            Map response = webClient.post()
                    .uri("/analyze")
                    .bodyValue(mlRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            double overallScore = ((Number) response.get("overallScore")).doubleValue();
            score.setOverallScore(BigDecimal.valueOf(overallScore));
            score.setMatchedKeywords(toJson(response.get("matchedKeywords")));
            score.setMissingKeywords(toJson(response.get("missingKeywords")));
            score.setRecommendationsSummary((String) response.get("recommendationsSummary"));

        } catch (Exception e) {
            score.setOverallScore(BigDecimal.ZERO);
            score.setMatchedKeywords("[]");
            score.setMissingKeywords("[]");
            score.setRecommendationsSummary("Scoring unavailable — ML service is offline.");
        }

        scoreRepository.save(score);
        return score;
    }

    @Transactional
    public ScoreResponse generateScore(Long resumeId, Long jobId) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() ->
                        new ResumeNotFoundException("Resume not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new JobNotFoundException("Job not found"));

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("Authenticated user not found"));

        if (!resume.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException(
                    "You are not allowed to access this resume");
        }

        Score score = generateScore(resume, job);

        return mapToResponse(score);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Transactional
    public ScoreResponse getScoreByResume(Long resumeId, Long jobId) {
        Score score = scoreRepository.findByResumeIdAndJobId(resumeId, jobId)
                .orElseThrow(() -> new ScoreNotFoundException(
                        "Score not found for resume id: " + resumeId));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        if (!score.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to view this score");
        }

        return mapToResponse(score);
    }

    @Transactional
    public List<ScoreResponse> getMyScores() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        return scoreRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ScoreResponse mapToResponse(Score score) {
        return new ScoreResponse(
                score.getId(),
                score.getUser().getId(),
                score.getJob().getId(),
                score.getResume().getId(),
                score.getOverallScore(),
                score.getMatchedKeywords(),
                score.getMissingKeywords(),
                score.getRecommendationsSummary()
        );
    }

    private Score buildNewScore(Resume resume, Job job) {
        Score score = new Score();
        score.setUser(resume.getUser());
        score.setJob(job);
        score.setResume(resume);
        return score;
    }

}