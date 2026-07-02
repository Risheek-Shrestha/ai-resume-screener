package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.JobPageResponse;
import com.risheek.resume_screener.dto.JobRequest;
import com.risheek.resume_screener.dto.JobResponse;
import com.risheek.resume_screener.entity.ApplicationWindowStatus;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.exception.InvalidApplicationWindowException;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.JobSkillRepository;
import com.risheek.resume_screener.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import com.risheek.resume_screener.entity.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final UserRepository userRepository;

    public JobService(JobRepository jobRepository, JobSkillRepository jobSkillRepository, UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public JobResponse createJob(JobRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with email: " + email));
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setJobType(request.getJobType());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setUser(currentUser);
        validateApplicationWindow(request);
        job.setApplicationStartsAt(request.getApplicationStartsAt());
        job.setApplicationDeadline(request.getApplicationDeadline());


        Job savedJob = jobRepository.save(job);
        saveSkills(savedJob, request.getSkills());
        return toResponse(savedJob);
    }

    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public JobResponse updateJob(Long id, JobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setJobType(request.getJobType());
        job.setExperienceLevel(request.getExperienceLevel());
        validateApplicationWindow(request);
        job.setApplicationDeadline(request.getApplicationDeadline());
        job.setApplicationStartsAt(request.getApplicationStartsAt());


        Job savedJob = jobRepository.save(job);

        jobSkillRepository.deleteByJobId(id);
        saveSkills(savedJob, request.getSkills());

        return toResponse(savedJob);
    }

    @Cacheable(
            value = "jobs",
            key = "'my_jobs_' + authentication.name + '_page_' + #page + '_size_' + #size"
    )
    @Transactional
    public JobPageResponse getMyJobs(int page, int size) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("Authenticated user not found"));

        Pageable pageable = PageRequest.of(page, size);

        Page<JobResponse> jobs = jobRepository
                .findByUserId(currentUser.getId(), pageable)
                .map(this::toResponse);

        return new JobPageResponse(
                jobs.getContent(),
                jobs.getNumber(),
                jobs.getSize(),
                jobs.getTotalElements(),
                jobs.getTotalPages(),
                jobs.isLast()
        );
    }

    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public void deleteJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));
        jobSkillRepository.deleteByJobId(id);
        jobRepository.delete(job);
    }

    @Cacheable(value = "jobs", key = "#id")
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));
        return toResponse(job);
    }

    @Cacheable(value = "jobs", key = "'page_' + #page + '_size_' + #size")
    public JobPageResponse getAllJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobResponse> jobPage = jobRepository.findAll(pageable)
                .map(this::toResponse);

        return new JobPageResponse(
                jobPage.getContent(),
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isLast()
        );
    }

    public JobPageResponse getOpenJobsForUser(int page, int size){

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with email: " + email));

        Pageable pageable = PageRequest.of(page, size);
        Page<JobResponse> jobPage = jobRepository.findOpenJobsNotAppliedByUser(currentUser.getId(), pageable)
                .map(this::toResponse);

        return new JobPageResponse(
                jobPage.getContent(),
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isLast()
        );

    }

    private void saveSkills(Job job, List<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) return;
        List<JobSkill> skills = skillNames.stream()
                .map(name -> new JobSkill(null, job, name))
                .toList();
        jobSkillRepository.saveAll(skills);
    }

    private JobResponse toResponse(Job job) {
        List<String> skillNames = jobSkillRepository.findByJobId(job.getId())
                .stream()
                .map(JobSkill::getSkillName)
                .toList();

        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getJobType(),
                job.getExperienceLevel(),
                skillNames,
                job.getCreatedAt(),
                job.getApplicationStartsAt(),
                job.getApplicationDeadline(),
                getApplicationStatus(job)
        );
    }

    private void validateApplicationWindow(JobRequest request) {
        if (!request.getApplicationStartsAt()
                .isBefore(request.getApplicationDeadline())) {

            throw new InvalidApplicationWindowException(
                    "Application start time must be before application deadline");
        }
    }

    private ApplicationWindowStatus getApplicationStatus(Job job) {

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(job.getApplicationStartsAt())) {
            return ApplicationWindowStatus.NOT_STARTED;
        }

        if (now.isAfter(job.getApplicationDeadline())) {
            return ApplicationWindowStatus.CLOSED;
        }

        return ApplicationWindowStatus.OPEN;
    }
}