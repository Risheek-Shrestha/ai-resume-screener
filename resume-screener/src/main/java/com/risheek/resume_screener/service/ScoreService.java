package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.Score;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.ScoreRepository;
import com.risheek.resume_screener.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;

    public ScoreService(ScoreRepository scoreRepository, UserRepository userRepository) {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void generateScore(Resume resume) {

        Score score = new Score();

        score.setUser(resume.getUser());
        score.setJob(resume.getJob());
        score.setResume(resume);

        score.setOverallScore(BigDecimal.valueOf(75.00));

        score.setMatchedKeywords("[]");
        score.setMissingKeywords("[]");

        score.setRecommendationsSummary(
                "Scoring engine not implemented yet"
        );

        scoreRepository.save(score);
    }

    public ScoreResponse getScoreByResume(Long resumeId) {

        Score score = scoreRepository.findByResumeId(resumeId)
                .orElseThrow(() ->
                        new ScoreNotFoundException(
                                "Score not found for resume id: " + resumeId));
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));
        if (!score.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException(
                    "You are not allowed to view this score");
        }

        return mapToResponse(score);
    }

    public List<ScoreResponse> getMyScores() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found"));

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
}
