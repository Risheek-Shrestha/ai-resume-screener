package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.*;
import com.risheek.resume_screener.entity.Application;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.ApplicationNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.ApplicationRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClientException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final WebClient webClient;

    public ResumeService(ResumeRepository resumeRepository, UserRepository userRepository, ApplicationRepository applicationRepository,
                         WebClient.Builder webClientBuilder, @Value("${ml.service.url}") String mlServiceUrl) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.webClient = webClientBuilder.baseUrl(mlServiceUrl).build();
    }

    @Transactional
    public ResumeResponse uploadResume( MultipartFile file, String resumeName) throws IOException {
        Resume resume = null;
        Resume savedResume;
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
        resume = new Resume();
        resume.setUser(currentUser);
        resume.setFileName(file.getOriginalFilename());
        resume.setFileType(file.getContentType());
        byte[] fileData = file.getBytes();
        resume.setFileData(fileData);
        resume.setResumeName(resumeName);

        parseAndSetText(resume, fileData, file.getContentType());

        savedResume = resumeRepository.save(resume);

        return new ResumeResponse(
                savedResume.getId(),
                savedResume.getFileName(),
                savedResume.getFileType(),
                savedResume.getResumeName(),
                savedResume.getFileData()
        );
    }

    @Transactional
    public ResumeResponse updateResume(Long id, MultipartFile file, String resumeName) throws IOException{
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found with id" + id));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));

        if (!resume.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to modify this resume");
        }

        resume.setUser(currentUser);
        resume.setFileName(file.getOriginalFilename());
        resume.setFileType(file.getContentType());
        byte[] fileData = file.getBytes();
        resume.setFileData(fileData);
        resume.setResumeName(resumeName);

        parseAndSetText(resume, fileData, file.getContentType());

        Resume savedResume = resumeRepository.save(resume);

        return new ResumeResponse(
                savedResume.getId(),
                savedResume.getFileName(),
                savedResume.getFileType(),
                savedResume.getResumeName(),
                savedResume.getFileData()
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
                resumeRepository.findByUserIdOrderByUploadedAtDesc(currentUser.getId());

        return resumes.stream()
                .map(resume -> new ResumeResponse(
                        resume.getId(),
                        resume.getResumeName(),
                        resume.getFileName(),
                        resume.getFileType(),
                        resume.getFileData()
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

        ResumeResponse response = new ResumeResponse();
        response.setId(resume.getId());
        response.setResumeName(resume.getResumeName());
        response.setFileName(resume.getFileName());
        response.setFileType(resume.getFileType());

        return response;
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

    @Transactional
    public ResumeResponse getResumeForApplication(Long applicationId) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() ->
                        new ApplicationNotFoundException("Application not found"));

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User employer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("Authenticated user not found"));

        if (!application.getJob().getUser().getId().equals(employer.getId())) {
            throw new UnauthorizedAccessException(
                    "You are not allowed to view this resume");
        }

        return toResponse(application.getResume());
    }

    private ResumeResponse toResponse(Resume resume) {

        return new ResumeResponse(
                resume.getId(),
                resume.getResumeName(),
                resume.getFileName(),
                resume.getFileType(),
                resume.getFileData()
        );
    }
}
