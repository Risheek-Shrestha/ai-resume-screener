package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.*;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ScoreService scoreService;
    private final WebClient webClient;

    public ResumeService(ResumeRepository resumeRepository, UserRepository userRepository,
                         JobRepository jobRepository, ScoreService scoreService,
                         WebClient.Builder webClientBuilder, @Value("${ml.service.url}") String mlServiceUrl) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.scoreService = scoreService;
        this.webClient = webClientBuilder.baseUrl(mlServiceUrl).build();
    }

    @Transactional
    public ResumeResponse uploadResume(@Valid ResumeRequest request) {
        Resume resume = null;
        Resume savedResume;
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
        Job currentJob = jobRepository.findById(request.getJobId())
                .orElseThrow(() ->
                        new JobNotFoundException("Job not found"));
        resume = new Resume();
        resume.setUser(currentUser);
        resume.setJob(currentJob);
        resume.setFileName(request.getFileName());
        resume.setFileType(request.getFileType());
        resume.setFileData(request.getFileData());

        parseAndSetText(resume, request.getFileData(), request.getFileType());

        savedResume = resumeRepository.save(resume);

        if (savedResume.getParsedTextAvailable()) {
            scoreService.generateScore(savedResume);
        }

        return new ResumeResponse(
                savedResume.getId(),
                savedResume.getJob().getId(),
                savedResume.getFileName(),
                savedResume.getFileType()
        );
    }

    @Transactional
    public ResumeResponse updateResume(Long id, @Valid ResumeRequest request) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found with id" + id));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
        Job currentJob = jobRepository.findById(request.getJobId())
                .orElseThrow(() ->
                        new JobNotFoundException("Job not found"));

        if (!resume.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to modify this resume");
        }

        resume.setUser(currentUser);
        resume.setJob(currentJob);
        resume.setFileName(request.getFileName());
        resume.setFileType(request.getFileType());
        resume.setFileData(request.getFileData());

        parseAndSetText(resume, request.getFileData(), request.getFileType());

        Resume savedResume = resumeRepository.save(resume);

        if (savedResume.getParsedTextAvailable()) {
            scoreService.generateScore(savedResume);
        }

        return new ResumeResponse(
                savedResume.getId(),
                savedResume.getJob().getId(),
                savedResume.getFileName(),
                savedResume.getFileType()
        );
    }

    @Transactional
    public void deleteResume(Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found"));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
        if (!resume.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to delete this resume");
        }
        resumeRepository.delete(resume);
    }

    @Transactional
    public List<ResumeResponse> getMyResumes() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Authenticated user not found"));

        List<Resume> resumes =
                resumeRepository.findByUserId(currentUser.getId());

        return resumes.stream()
                .map(resume -> new ResumeResponse(
                        resume.getId(),
                        resume.getJob().getId(),
                        resume.getFileName(),
                        resume.getFileType()
                ))
                .toList();
    }

    @Transactional
    public ResumeResponse getResumeById(Long id) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Authenticated user not found"));

        Resume resume = resumeRepository
                .findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() ->
                        new ResumeNotFoundException("Resume not found"));

        return new ResumeResponse(
                resume.getId(),
                resume.getJob().getId(),
                resume.getFileName(),
                resume.getFileType()
        );
    }

    private List<Integer> toIntList(byte[] bytes) {
        Integer[] result = new Integer[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i] & 0xFF;
        }

        return Arrays.asList(result);
    }

    private void parseAndSetText(Resume resume, byte[] fileData, String fileType) {
        Map<String, Object> extractRequest = Map.of(
                "fileData", toIntList(fileData),
                "fileType", fileType
        );
        try {
            ParsedTextResponse parsedResponse = webClient.post()
                    .uri("/extract-text")
                    .bodyValue(extractRequest)
                    .retrieve()
                    .bodyToMono(ParsedTextResponse.class)
                    .block();

            resume.setParsedText(parsedResponse.getParsedText());
            resume.setParsedTextAvailable(true);
        } catch (WebClientException e) {
            resume.setParsedText("");
            resume.setParsedTextAvailable(false);
        }
    }

}
