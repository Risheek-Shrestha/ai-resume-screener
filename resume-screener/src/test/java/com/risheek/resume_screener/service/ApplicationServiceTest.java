package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ApplicationRequest;
import com.risheek.resume_screener.dto.ApplicationResponse;
import com.risheek.resume_screener.entity.*;
import com.risheek.resume_screener.exception.ApplicationNotFoundException;
import com.risheek.resume_screener.exception.InvalidApplicationStatusException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private UserRepository userRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private ScoreService scoreService;
    @Mock private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                applicationRepository, resumeRepository, jobRepository,
                scoreService, scoreRepository, userRepository, rabbitTemplate);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", null));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void testApplyForJob_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(1L, null));
        assertEquals("Authenticated user not found", ex.getMessage());
    }

    @Test
    void testApplyForJob_JobNotFound() {
        User user = new User(); user.setId(1L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(1L, null));
        assertEquals("Job not found", ex.getMessage());
    }

    @Test
    void testApplyForJob_ResumeNotFound() {
        User user = new User(); user.setId(1L);
        Job job = new Job(); job.setId(10L);
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));
        assertEquals("Resume not found or does not belong to user", ex.getMessage());
    }

    @Test
    void testApplyForJob_NotStarted() {
        User user = new User(); user.setId(1L);
        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().plusDays(2));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(10));
        Resume resume = new Resume();
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(resume));

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));
        assertEquals("Applications are not open for this job", ex.getMessage());
    }

    @Test
    void testApplyForJob_Closed() {
        User user = new User(); user.setId(1L);
        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(10));
        job.setApplicationDeadline(LocalDateTime.now().minusDays(1));
        Resume resume = new Resume();
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(resume));

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));
        assertEquals("Applications are closed for this job", ex.getMessage());
    }

    @Test
    void testApplyForJob_WrongCourse_Rejected() {
        Course requiredCourse = new Course();
        requiredCourse.setId(1L);
        requiredCourse.setName("Computer Science");

        Course userCourse = new Course();
        userCourse.setId(2L);
        userCourse.setName("Mechanical Engineering");

        User user = new User(); user.setId(1L);
        user.setCurrentCourse(userCourse);

        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        job.getEligibleCourses().add(requiredCourse);

        Resume resume = new Resume();
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(resume));

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));
        assertEquals("This job is only open to students of specific courses", ex.getMessage());
    }

    @Test
    void testApplyForJob_NoCourseSet_RejectedWhenJobRestrictsByCourse() {
        Course requiredCourse = new Course();
        requiredCourse.setId(1L);
        requiredCourse.setName("Computer Science");

        User user = new User(); user.setId(1L);
        // currentCourse intentionally left null

        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        job.getEligibleCourses().add(requiredCourse);

        Resume resume = new Resume();
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(resume));

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));
        assertEquals("This job is only open to students of specific courses", ex.getMessage());
    }

    @Test
    void testApplyForJob_WrongSemester_Rejected() {
        User user = new User(); user.setId(1L);
        user.setCurrentSemester(3);

        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        job.getEligibleSemesters().add(5);
        job.getEligibleSemesters().add(6);

        Resume resume = new Resume();
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(resume));

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));
        assertEquals("This job is only open to students in specific semesters", ex.getMessage());
    }

    @Test
    void testApplyForJob_EligibleCourseAndSemester_Allowed() {
        Course course = new Course();
        course.setId(1L);
        course.setName("Computer Science");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setCurrentCourse(course);
        user.setCurrentSemester(5);

        Job job = new Job();
        job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        job.getEligibleCourses().add(course);
        job.getEligibleSemesters().add(5);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);

        Score score = new Score();
        score.setId(50L);
        score.setResume(resume);
        score.setOverallScore(BigDecimal.valueOf(80));

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByUserIdAndJobIdAndStatusNot(1L, 10L, ApplicationStatus.REJECTED)).thenReturn(false);
        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.of(score));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.applyForJob(10L, request);

        assertNotNull(result);
        assertEquals(ApplicationStatus.APPLIED, result.getStatus());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void testApplyForJob_NoRestrictions_OpenToAnyone() {
        // A job with empty eligibleCourses/eligibleSemesters (the default)
        // should accept any user regardless of their course/semester.
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        // currentCourse and currentSemester intentionally left unset

        Job job = new Job();
        job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);

        Score score = new Score();
        score.setId(50L);
        score.setResume(resume);
        score.setOverallScore(BigDecimal.valueOf(80));

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByUserIdAndJobIdAndStatusNot(1L, 10L, ApplicationStatus.REJECTED)).thenReturn(false);
        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.of(score));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.applyForJob(10L, request);

        assertNotNull(result);
        assertEquals(ApplicationStatus.APPLIED, result.getStatus());
    }

    @Test
    void testApplyForJob_DuplicateApplication() {
        User user = new User(); user.setId(1L);
        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        Resume resume = new Resume();
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByUserIdAndJobIdAndStatusNot(anyLong(), anyLong(), eq(ApplicationStatus.REJECTED)))
                .thenReturn(true);

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));
        assertEquals("User already has an active application for this job", ex.getMessage());
    }

    @Test
    void testApplyForJob_AllowsReapplyAfterRejection() {
        // A user whose only prior application for this job was REJECTED
        // should be able to apply again - existsByUserIdAndJobIdAndStatusNot
        // (excluding REJECTED) correctly returns false in that case.
        User user = new User(); user.setId(1L);
        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        Resume resume = new Resume(); resume.setId(101L); resume.setUser(user);
        Score score = new Score(); score.setOverallScore(BigDecimal.valueOf(80));
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(101L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(101L, 1L)).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByUserIdAndJobIdAndStatusNot(1L, 10L, ApplicationStatus.REJECTED))
                .thenReturn(false);
        when(scoreRepository.findByResumeIdAndJobId(101L, 10L)).thenReturn(Optional.of(score));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.applyForJob(10L, request);

        assertNotNull(result);
        assertEquals(ApplicationStatus.APPLIED, result.getStatus());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void testApplyForJob_ScoreGeneratedLazily_WhenNotFound() {
        User user = new User(); user.setId(1L);
        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        Resume resume = new Resume(); resume.setId(100L);
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        Score generatedScore = new Score();
        generatedScore.setId(50L);
        generatedScore.setOverallScore(BigDecimal.valueOf(75));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByUserIdAndJobIdAndStatusNot(anyLong(), anyLong(), eq(ApplicationStatus.REJECTED))).thenReturn(false);
        when(scoreRepository.findByResumeIdAndJobId(100L, 10L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(generatedScore));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.applyForJob(10L, request);

        verify(scoreService).generateScore(resume, job);
        assertNotNull(result);
        assertEquals(generatedScore, result.getScore());
    }

    @Test
    void testApplyForJob_HappyPath() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setUser(user);

        Score score = new Score();
        score.setId(50L);
        score.setResume(resume);
        score.setOverallScore(BigDecimal.valueOf(71));

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByUserIdAndJobIdAndStatusNot(1L, 10L, ApplicationStatus.REJECTED)).thenReturn(false);
        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.of(score));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.applyForJob(10L, request);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(job, result.getJob());
        assertEquals(resume, result.getResume());
        assertEquals(score, result.getScore());
        assertEquals(ApplicationStatus.APPLIED, result.getStatus());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void testApplyForJob_ScoreBelowThreshold_RejectsApplication() {
        User user = new User(); user.setId(1L);
        Job job = new Job(); job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));
        Resume resume = new Resume(); resume.setId(100L); resume.setUser(user);
        Score score = new Score(); score.setOverallScore(BigDecimal.valueOf(45));
        ApplicationRequest request = new ApplicationRequest(); request.setResumeId(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(resume));
        when(applicationRepository.existsByUserIdAndJobIdAndStatusNot(1L, 10L, ApplicationStatus.REJECTED)).thenReturn(false);
        when(scoreRepository.findByResumeIdAndJobId(100L, 10L)).thenReturn(Optional.of(score));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.applyForJob(10L, request);

        assertEquals(ApplicationStatus.REJECTED, result.getStatus());
    }

    @Test
    void testGetMyApplications_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.getMyApplications());
        assertEquals("Authenticated user not found", ex.getMessage());
    }

    @Test
    void testGetMyApplications_Success() {
        User user = new User(); user.setId(1L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserId(1L)).thenReturn(List.of());

        List<ApplicationResponse> responses = applicationService.getMyApplications();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testGetApplicationsForJob_JobNotFound() {
        when(jobRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsForJob(1L));
        assertEquals("Job not found", ex.getMessage());

        verify(applicationRepository, never()).findByJobId(anyLong());
    }

    @Test
    void testGetApplicationsForJob_Unauthorized() {
        User owner = new User(); owner.setId(5L);
        User current = new User(); current.setId(1L);
        Job job = new Job(); job.setUser(owner);

        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(current));

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsForJob(1L));
        assertEquals("You are not allowed to view applications for this job", ex.getMessage());
    }

    @Test
    void testGetApplicationsForJob_Success() {
        User employer = new User(); employer.setId(1L);
        Job job = new Job(); job.setId(10L); job.setUser(employer);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(employer));
        when(applicationRepository.findByJobId(10L)).thenReturn(List.of());

        List<ApplicationResponse> responses = applicationService.getApplicationsForJob(10L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testGetAcceptedApplicationsForJob_JobNotFound() {
        when(jobRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.getAcceptedApplicationsForJob(1L));
        assertEquals("Job not found", ex.getMessage());
    }

    @Test
    void testGetAcceptedApplicationsForJob_Unauthorized() {
        User owner = new User(); owner.setId(5L);
        User current = new User(); current.setId(1L);
        Job job = new Job(); job.setUser(owner);

        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(current));

        Exception ex = assertThrows(RuntimeException.class,
                () -> applicationService.getAcceptedApplicationsForJob(1L));
        assertEquals("You are not allowed to view applications for this job", ex.getMessage());
    }

    @Test
    void testGetAcceptedApplicationsForJob_Success() {
        User employer = new User(); employer.setId(1L);
        Job job = new Job(); job.setId(10L); job.setUser(employer);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(employer));
        when(applicationRepository.findByJobIdAndStatusOrderByScoreOverallScoreDesc(
                10L, ApplicationStatus.HIRED)).thenReturn(List.of());

        List<ApplicationResponse> responses = applicationService.getAcceptedApplicationsForJob(10L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(applicationRepository)
                .findByJobIdAndStatusOrderByScoreOverallScoreDesc(10L, ApplicationStatus.HIRED);
    }

    @Test
    void testUpdateApplicationStatus_ApplicationNotFound() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(ApplicationNotFoundException.class,
                () -> applicationService.updateApplicationStatus(1L, ApplicationStatus.SHORTLISTED));
        assertEquals("Application not found", ex.getMessage());
    }

    @Test
    void testUpdateApplicationStatus_UserNotFound() {
        Application application = new Application();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> applicationService.updateApplicationStatus(1L, ApplicationStatus.SHORTLISTED));
    }

    @Test
    void testUpdateApplicationStatus_Unauthorized() {
        User owner = new User(); owner.setId(5L);
        User current = new User(); current.setId(1L);
        Job job = new Job(); job.setUser(owner);
        Application application = new Application(); application.setJob(job);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(current));

        assertThrows(UnauthorizedAccessException.class,
                () -> applicationService.updateApplicationStatus(1L, ApplicationStatus.SHORTLISTED));
    }

    @Test
    void testUpdateApplicationStatus_AppliedToShortlisted() {
        User admin = new User(); admin.setId(1L);
        Job job = new Job(); job.setId(10L); job.setTitle("Backend"); job.setUser(admin);
        Resume resume = new Resume(); resume.setId(100L);
        Score score = new Score(); score.setOverallScore(BigDecimal.valueOf(85));
        User applicant = new User(); applicant.setId(2L); applicant.setEmail("applicant@example.com");

        Application application = new Application();
        application.setId(1L);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setJob(job); application.setResume(resume);
        application.setScore(score); application.setUser(applicant);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationResponse response = applicationService.updateApplicationStatus(1L, ApplicationStatus.SHORTLISTED);

        assertEquals(ApplicationStatus.SHORTLISTED, response.getStatus());
        verify(applicationRepository).save(application);
    }

    @Test
    void testUpdateApplicationStatus_AppliedToRejected() {
        User admin = new User(); admin.setId(1L);
        Job job = new Job(); job.setId(10L); job.setTitle("Backend"); job.setUser(admin);
        Resume resume = new Resume(); resume.setId(100L);
        Score score = new Score(); score.setOverallScore(BigDecimal.valueOf(85));
        User applicant = new User(); applicant.setId(2L); applicant.setEmail("applicant@example.com");

        Application application = new Application();
        application.setId(1L); application.setStatus(ApplicationStatus.APPLIED);
        application.setJob(job); application.setResume(resume);
        application.setScore(score); application.setUser(applicant);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationResponse response = applicationService.updateApplicationStatus(1L, ApplicationStatus.REJECTED);
        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
    }

    @Test
    void testUpdateApplicationStatus_ShortlistedToHired() {
        User admin = new User(); admin.setId(1L);
        Job job = new Job(); job.setId(10L); job.setTitle("Backend"); job.setUser(admin);
        Resume resume = new Resume(); resume.setId(100L);
        Score score = new Score(); score.setOverallScore(BigDecimal.valueOf(85));
        User applicant = new User(); applicant.setId(2L); applicant.setEmail("applicant@example.com");

        Application application = new Application();
        application.setId(1L); application.setStatus(ApplicationStatus.SHORTLISTED);
        application.setJob(job); application.setResume(resume);
        application.setScore(score); application.setUser(applicant);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationResponse response = applicationService.updateApplicationStatus(1L, ApplicationStatus.HIRED);
        assertEquals(ApplicationStatus.HIRED, response.getStatus());
    }

    @Test
    void testUpdateApplicationStatus_ShortlistedToRejected() {
        User admin = new User(); admin.setId(1L);
        Job job = new Job(); job.setId(10L); job.setTitle("Backend"); job.setUser(admin);
        Resume resume = new Resume(); resume.setId(100L);
        Score score = new Score(); score.setOverallScore(BigDecimal.valueOf(85));
        User applicant = new User(); applicant.setId(2L); applicant.setEmail("applicant@example.com");

        Application application = new Application();
        application.setId(1L); application.setStatus(ApplicationStatus.SHORTLISTED);
        application.setJob(job); application.setResume(resume);
        application.setScore(score); application.setUser(applicant);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationResponse response = applicationService.updateApplicationStatus(1L, ApplicationStatus.REJECTED);
        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
    }

    @Test
    void testUpdateApplicationStatus_AppliedToHired_Invalid() {
        User admin = new User(); admin.setId(1L);
        Job job = new Job(); job.setUser(admin);
        Application application = new Application();
        application.setStatus(ApplicationStatus.APPLIED); application.setJob(job);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));

        Exception ex = assertThrows(InvalidApplicationStatusException.class,
                () -> applicationService.updateApplicationStatus(1L, ApplicationStatus.HIRED));
        assertEquals("Application can only move from APPLIED to SHORTLISTED or REJECTED", ex.getMessage());
    }

    @Test
    void testUpdateApplicationStatus_ShortlistedToApply_Invalid() {
        User admin = new User(); admin.setId(1L);
        Job job = new Job(); job.setUser(admin);
        Application application = new Application();
        application.setStatus(ApplicationStatus.SHORTLISTED); application.setJob(job);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));

        Exception ex = assertThrows(InvalidApplicationStatusException.class,
                () -> applicationService.updateApplicationStatus(1L, ApplicationStatus.APPLIED));
        assertEquals("Shortlisted application can only move to HIRED or REJECTED", ex.getMessage());
    }

    @Test
    void testUpdateApplicationStatus_HiredToRejected_Invalid() {
        User admin = new User(); admin.setId(1L);
        Job job = new Job(); job.setUser(admin);
        Application application = new Application();
        application.setStatus(ApplicationStatus.HIRED); application.setJob(job);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));

        Exception ex = assertThrows(InvalidApplicationStatusException.class,
                () -> applicationService.updateApplicationStatus(1L, ApplicationStatus.REJECTED));
        assertEquals("Application is already in a terminal state", ex.getMessage());
    }

    @Test
    void testUpdateApplicationStatus_RejectedToShortlisted_Invalid() {
        User admin = new User(); admin.setId(1L);
        Job job = new Job(); job.setUser(admin);
        Application application = new Application();
        application.setStatus(ApplicationStatus.REJECTED); application.setJob(job);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));

        Exception ex = assertThrows(InvalidApplicationStatusException.class,
                () -> applicationService.updateApplicationStatus(1L, ApplicationStatus.SHORTLISTED));
        assertEquals("Application is already in a terminal state", ex.getMessage());
    }
}
