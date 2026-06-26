package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ApplicationRequest;
import com.risheek.resume_screener.dto.ApplicationResponse;
import com.risheek.resume_screener.entity.*;
import com.risheek.resume_screener.exception.*;
import com.risheek.resume_screener.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;

    public ApplicationService(ApplicationRepository applicationRepository, ResumeRepository resumeRepository,
                              JobRepository jobRepository, ScoreRepository scoreRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
    }

    public void applyForJob(Long jobId, ApplicationRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
        Job currentJob = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new JobNotFoundException("Job not found"));

        Resume resume = resumeRepository.findByIdAndUserId(request.getResumeId(), currentUser.getId())
                .orElseThrow(() -> new UnauthorizedAccessException("Resume not found or does not belong to user"));

        if (!resume.getJob().getId().equals(currentJob.getId())) {
            throw new UnauthorizedAccessException("Resume does not belong to the specified job");
        }

        if (currentJob.getApplicationStartsAt() != null && currentJob.getApplicationDeadline() != null) {
            if (currentJob.getApplicationStartsAt().isAfter(java.time.LocalDateTime.now())) {
                throw new ApplicationNotStartedException("Applications are not open for this job");
            } else if (currentJob.getApplicationDeadline().isBefore(java.time.LocalDateTime.now())) {
                throw new ApplicationClosedException("Applications are closed for this job");
            }
        }

        if (applicationRepository.existsByUserIdAndJobId(currentUser.getId(), currentJob.getId())) {
            throw new DuplicateApplicationException("User has already applied for this job");
        }

        Score score = scoreRepository.findByResumeId(resume.getId())
                .orElseThrow(() -> new ScoreNotFoundException("Score not found for the resume"));

        Application application = new Application();
        application.setUser(currentUser);
        application.setJob(currentJob);
        application.setResume(resume);
        application.setScore(score);application.setStatus(
                score.getOverallScore().compareTo(BigDecimal.valueOf(50)) > 0
                        ? ApplicationStatus.APPLIED
                        : ApplicationStatus.REJECTED
        );
        applicationRepository.save(application);

    }

    public List<ApplicationResponse> getMyApplications() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));

        return applicationRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ApplicationResponse> getApplicationsForJob(Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));

        if (!job.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException(
                    "You are not allowed to view applications for this job");
        }

        return applicationRepository.findByJobId(jobId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ApplicationResponse> getAcceptedApplicationsForJob(Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));

        if (!job.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException(
                    "You are not allowed to view applications for this job");
        }

        return applicationRepository
                .findByJobIdAndStatusOrderByScoreOverallScoreDesc(
                        jobId,
                        ApplicationStatus.APPLIED
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ApplicationResponse mapToResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getResume().getId(),
                application.getScore() != null ? application.getScore().getOverallScore() : null,
                application.getStatus(),
                application.getAppliedAt()
        );
    }
}