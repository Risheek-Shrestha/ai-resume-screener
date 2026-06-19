package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.JobRequest;
import com.risheek.resume_screener.dto.JobResponse;
import com.risheek.resume_screener.dto.ResumeRequest;
import com.risheek.resume_screener.dto.ResumeResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.ScoreRepository;
import com.risheek.resume_screener.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ScoreRepository scoreRepository;
    private final ScoreService scoreService;

    public ResumeService(ResumeRepository resumeRepository, UserRepository userRepository,
                         JobRepository jobRepository, ScoreRepository scoreRepository,
                         ScoreService scoreService){
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.scoreRepository = scoreRepository;
        this.scoreService = scoreService;
    }

    @Transactional
    public ResumeResponse uploadResume(@Valid ResumeRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Authenticated user not found"));
        Job currentJob = jobRepository.findById(request.getJobId())
                .orElseThrow(() ->
                        new JobNotFoundException("Job not found"));
        Resume resume = new Resume();
        resume.setUser(currentUser);
        resume.setJob(currentJob);
        resume.setFileName(request.getFileName());
        resume.setFileType(request.getFileType());
        resume.setFileData(request.getFileData());

        Resume savedResume = resumeRepository.save(resume);

        scoreService.generateScore(savedResume);

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
                .orElseThrow(()-> new ResumeNotFoundException("Resume not found with id" + id));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Authenticated user not found"));
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

        Resume savedResume = resumeRepository.save(resume);
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
                .orElseThrow(()-> new UsernameNotFoundException("Authenticated user not found"));
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
}
