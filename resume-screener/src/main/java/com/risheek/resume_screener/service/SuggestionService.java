package com.risheek.resume_screener.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.dto.JobSuggestionResponse;
import com.risheek.resume_screener.dto.SuggestionResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.Score;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.JobSkillRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.ScoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SuggestionService {

    private final ScoreRepository scoreRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public SuggestionService(ScoreRepository scoreRepository,
                             ResumeRepository resumeRepository,
                             JobRepository jobRepository,
                             JobSkillRepository jobSkillRepository,
                             ObjectMapper objectMapper,
                             WebClient.Builder webClientBuilder,
                             @Value("${ml.service.url}") String mlServiceUrl) {
        this.scoreRepository = scoreRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.baseUrl(mlServiceUrl).build();
    }

    public SuggestionResponse getImprovementSuggestions(Long resumeId, Long jobId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found"));

        if (!resume.getUser().getEmail().equals(email)) {
            throw new UnauthorizedAccessException("You are not allowed to view this resume");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));
        Score score = scoreRepository.findByResumeIdAndJobId(resumeId, jobId)
                .orElseThrow(() -> new ScoreNotFoundException("Score not found for resume: " + resumeId));

        List<String> matchedKeywords = parseJsonList(score.getMatchedKeywords());
        List<String> missingKeywords = parseJsonList(score.getMissingKeywords());
        List<String> jobSkills = jobSkillRepository.findByJobId(jobId)
                .stream().map(JobSkill::getSkillName).toList();

        Map<String, Object> mlRequest = Map.of(
                "resumeText", resume.getParsedText(),
                "jobSkills", jobSkills,
                "jobDescription", job.getDescription(),
                "jobTitle", job.getTitle(),
                "overallScore", score.getOverallScore().doubleValue(),
                "matchedKeywords", matchedKeywords,
                "missingKeywords", missingKeywords
        );

        try {
            Map response = webClient.post()
                    .uri("/suggest")
                    .bodyValue(mlRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return new SuggestionResponse(
                    resumeId,
                    score.getOverallScore(),
                    (String) response.get("scoreLevel"),
                    missingKeywords,
                    parseList(response.get("weakAreas")),
                    parseList(response.get("actionableSteps")),
                    parseList(response.get("suggestedLearningPaths")),
                    parseList(response.get("resumeImprovementTips"))
            );
        } catch (Exception e) {
            return new SuggestionResponse(
                    resumeId,
                    score.getOverallScore(),
                    score.getOverallScore().doubleValue() >= 80 ? "STRONG"
                            : score.getOverallScore().doubleValue() >= 50 ? "MODERATE" : "WEAK",
                    missingKeywords,
                    List.of("ML service unavailable — suggestions based on keyword analysis only"),
                    List.of("Address missing skills: " + String.join(", ", missingKeywords)),
                    List.of("Search for free resources on freeCodeCamp.org or YouTube"),
                    List.of("Tailor your resume to match the job description language")
            );
        }
    }

    public List<JobSuggestionResponse> getSuggestedJobs(Long resumeId , Long jobId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found"));

        if (!resume.getUser().getEmail().equals(email)) {
            throw new UnauthorizedAccessException("You are not allowed to view this resume");
        }

        Score score = scoreRepository.findByResumeIdAndJobId(resumeId, jobId)
                .orElseThrow(() -> new ScoreNotFoundException("Score not found for resume: " + resumeId));

        List<String> resumeSkills = parseJsonList(score.getMatchedKeywords());
        List<Job> allJobs = jobRepository.findAll();

        List<Map<String, Object>> jobCandidates = allJobs.stream()
                .map(job -> {
                    List<String> skills = jobSkillRepository.findByJobId(job.getId())
                            .stream().map(JobSkill::getSkillName).toList();
                    return (Map<String, Object>) Map.of(
                            "jobId", job.getId().intValue(),
                            "jobTitle", job.getTitle(),
                            "jobType", job.getJobType().name(),
                            "experienceLevel", job.getExperienceLevel().name(),
                            "jobSkills", skills,
                            "jobDescription", job.getDescription()
                    );
                })
                .toList();

        Map<String, Object> mlRequest = Map.of(
                "resume", Map.of(
                        "resumeText", resume.getParsedText(),
                        "resumeSkills", resumeSkills
                ),
                "jobs", jobCandidates
        );

        try {
            List<Map> results = webClient.post()
                    .uri("/match-jobs")
                    .bodyValue(mlRequest)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .block();

            return results.stream()
                    .map(r -> new JobSuggestionResponse(
                            ((Number) r.get("jobId")).longValue(),
                            (String) r.get("jobTitle"),
                            (String) r.get("jobType"),
                            (String) r.get("experienceLevel"),
                            ((Number) r.get("matchPercentage")).doubleValue(),
                            parseList(r.get("matchedSkills")),
                            parseList(r.get("missingSkills")),
                            ((Number) r.get("semanticSimilarity")).doubleValue(),
                            (String) r.get("whyMatch")
                    ))
                    .toList();
        } catch (Exception e) {
            return allJobs.stream()
                    .map(j -> fallbackJobMatch(j, resumeSkills))
                    .filter(r -> r.getMatchPercentage() > 0)
                    .sorted((a, b) -> Double.compare(b.getMatchPercentage(), a.getMatchPercentage()))
                    .limit(5)
                    .toList();
        }
    }

    private JobSuggestionResponse fallbackJobMatch(Job job, List<String> resumeSkills) {
        List<String> required = jobSkillRepository.findByJobId(job.getId())
                .stream().map(JobSkill::getSkillName).toList();
        List<String> matched = resumeSkills.stream()
                .filter(s -> required.stream().anyMatch(r -> r.equalsIgnoreCase(s)))
                .toList();
        List<String> missing = required.stream()
                .filter(r -> resumeSkills.stream().noneMatch(s -> s.equalsIgnoreCase(r)))
                .toList();
        double matchPct = required.isEmpty() ? 0 :
                Math.round((matched.size() / (double) required.size()) * 10000.0) / 100.0;

        return new JobSuggestionResponse(
                job.getId(), job.getTitle(), job.getJobType().name(),
                job.getExperienceLevel().name(), matchPct, matched, missing, 0.0,
                "Match based on keyword analysis only (ML service offline)"
        );
    }

    private List<String> parseJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseList(Object obj) {
        if (obj instanceof List) return (List<String>) obj;
        return new ArrayList<>();
    }
}
