package com.risheek.resume_screener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.Score;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.JobSkillRepository;
import com.risheek.resume_screener.repository.ScoreRepository;
import com.risheek.resume_screener.repository.UserRepository;
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

    public ScoreService(ScoreRepository scoreRepository,
                        UserRepository userRepository,
                        JobSkillRepository jobSkillRepository,
                        WebClient.Builder webClientBuilder,
                        ObjectMapper objectMapper,
                        @Value("${ml.service.url}") String mlServiceUrl) {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.webClient = webClientBuilder.baseUrl(mlServiceUrl).build();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void generateScore(Resume resume) {
        List<String> jobSkills = jobSkillRepository
                .findByJobId(resume.getJob().getId())
                .stream()
                .map(JobSkill::getSkillName)
                .toList();

        Map<String, Object> mlRequest = Map.of(
                "resumeText", resume.getParsedText(),
                "jobSkills", jobSkills,
                "jobDescription", resume.getJob().getDescription()
        );

        Score score = scoreRepository.findByResumeId(resume.getId())
                .orElseGet(() -> buildNewScore(resume));

        score.setUser(resume.getUser());
        score.setJob(resume.getJob());

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
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Transactional
    public ScoreResponse getScoreByResume(Long resumeId) {
        Score score = scoreRepository.findByResumeId(resumeId)
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

    private Score buildNewScore(Resume resume) {
        Score score = new Score();
        score.setUser(resume.getUser());
        score.setJob(resume.getJob());
        score.setResume(resume);
        return score;
    }

}