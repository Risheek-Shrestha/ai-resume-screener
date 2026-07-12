package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.CourseResponse;
import com.risheek.resume_screener.dto.JobPageResponse;
import com.risheek.resume_screener.dto.JobRequest;
import com.risheek.resume_screener.dto.JobResponse;
import com.risheek.resume_screener.entity.Application;
import com.risheek.resume_screener.entity.ApplicationStatus;
import com.risheek.resume_screener.entity.ApplicationWindowStatus;
import com.risheek.resume_screener.entity.Course;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.entity.NotificationType;
import com.risheek.resume_screener.exception.CourseNotFoundException;
import com.risheek.resume_screener.exception.InvalidApplicationWindowException;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.ApplicationRepository;
import com.risheek.resume_screener.repository.CourseRepository;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.JobSkillRepository;
import com.risheek.resume_screener.repository.UserRepository;
import com.risheek.resume_screener.specification.JobSpecification;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import com.risheek.resume_screener.entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationRepository applicationRepository;
    private final CourseRepository courseRepository;

    public JobService(JobRepository jobRepository, JobSkillRepository jobSkillRepository,
                       UserRepository userRepository, NotificationService notificationService,
                       ApplicationRepository applicationRepository, CourseRepository courseRepository) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.applicationRepository = applicationRepository;
        this.courseRepository = courseRepository;
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
        job.setEligibleCourses(resolveCourses(request.getEligibleCourseIds()));
        job.setEligibleSemesters(
                request.getEligibleSemesters() == null ? new HashSet<>() : new HashSet<>(request.getEligibleSemesters()));


        Job savedJob = jobRepository.save(job);
        saveSkills(savedJob, request.getSkills());

        notificationService.notifyAllUsers(
                NotificationType.JOB_POSTED,
                "New job posted: " + savedJob.getTitle(),
                "A new " + savedJob.getJobType().name().replace('_', ' ').toLowerCase() +
                        " position for " + savedJob.getExperienceLevel().name().toLowerCase() +
                        " candidates was just posted.",
                savedJob.getId()
        );

        return toResponse(savedJob);
    }

    @Transactional
    @CacheEvict(value = "jobs", allEntries = true)
    public JobResponse updateJob(Long id, JobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!job.getUser().getEmail().equals(currentEmail)) {
            throw new UnauthorizedAccessException("You can only edit jobs you posted");
        }

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setJobType(request.getJobType());
        job.setExperienceLevel(request.getExperienceLevel());
        validateApplicationWindow(request);
        job.setApplicationDeadline(request.getApplicationDeadline());
        job.setApplicationStartsAt(request.getApplicationStartsAt());
        job.setEligibleCourses(resolveCourses(request.getEligibleCourseIds()));
        job.setEligibleSemesters(
                request.getEligibleSemesters() == null ? new HashSet<>() : new HashSet<>(request.getEligibleSemesters()));


        Job savedJob = jobRepository.save(job);

        jobSkillRepository.deleteByJobId(id);
        saveSkills(savedJob, request.getSkills());

        return toResponse(savedJob);
    }

    @Cacheable(
            value = "jobs",
            key = "'my_jobs_' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '_page_' + #page + '_size_' + #size"
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

    @Cacheable(value = "jobs", key = "'page_' + #page + '_size_' + #size + 'keyword' + #keyword + 'jobType' + #jobType + 'level' + #level + 'skill' + #skill")
    public JobPageResponse getAllJobs(int page, int size, String keyword, Job.JobType jobType, Job.ExperienceLevel level, String skill) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<Job> spec = JobSpecification.buildSpecification(keyword, jobType, level, skill);
        Page<JobResponse> jobPage = jobRepository.findAll(spec, pageable)
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

    public JobPageResponse getOpenJobsForUser(int page, int size, String keyword, Job.JobType jobType, Job.ExperienceLevel level, String skill){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found with email: " + email));

        Pageable pageable = PageRequest.of(page, size);

        // Note: this intentionally does NOT filter out jobs the user has
        // already applied to. Open jobs always stay visible in the listing;
        // userApplicationStatus (below) tells the frontend whether to show
        // "Already Applied" (APPLIED/SHORTLISTED/HIRED) or let the user
        // apply again (REJECTED, or never applied).
        Specification<Job> spec = JobSpecification.buildSpecification(keyword, jobType, level, skill)
                .and(JobSpecification.isOpenNow());

        Page<JobResponse> jobPage = jobRepository.findAll(spec, pageable)
                .map(job -> toResponse(job, currentUser));

        return new JobPageResponse(
                jobPage.getContent(),
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isLast()
        );
    }

    private Set<Course> resolveCourses(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Course> courses = new HashSet<>();
        for (Long courseId : courseIds) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new CourseNotFoundException("Course not found: " + courseId));
            courses.add(course);
        }
        return courses;
    }

    private void saveSkills(Job job, List<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) return;
        List<JobSkill> skills = skillNames.stream()
                .map(name -> new JobSkill(null, job, name))
                .toList();
        jobSkillRepository.saveAll(skills);
    }

    private JobResponse toResponse(Job job) {
        return toResponse(job, (User) null);
    }

    // currentUser is only passed for user-specific listings (e.g. GET /jobs/open);
    // otherwise userApplicationStatus/eligibleForCurrentUser are left null since
    // those responses are shared/cached across users.
    private JobResponse toResponse(Job job, User currentUser) {
        List<String> skillNames = jobSkillRepository.findByJobId(job.getId())
                .stream()
                .map(JobSkill::getSkillName)
                .toList();

        ApplicationStatus userApplicationStatus = null;
        Boolean eligibleForCurrentUser = null;
        if (currentUser != null) {
            userApplicationStatus = applicationRepository
                    .findFirstByUserIdAndJobIdOrderByAppliedAtDesc(currentUser.getId(), job.getId())
                    .map(Application::getStatus)
                    .orElse(null);
            eligibleForCurrentUser = isEligible(job, currentUser);
        }

        List<CourseResponse> eligibleCourses = job.getEligibleCourses().stream()
                .map(CourseResponse::from)
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
                getApplicationStatus(job),
                userApplicationStatus,
                eligibleCourses,
                job.getEligibleSemesters(),
                eligibleForCurrentUser
        );
    }

    private boolean isEligible(Job job, User user) {
        if (!job.getEligibleCourses().isEmpty()) {
            if (user.getCurrentCourse() == null || !job.getEligibleCourses().contains(user.getCurrentCourse())) {
                return false;
            }
        }
        if (!job.getEligibleSemesters().isEmpty()) {
            if (user.getCurrentSemester() == null || !job.getEligibleSemesters().contains(user.getCurrentSemester())) {
                return false;
            }
        }
        return true;
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