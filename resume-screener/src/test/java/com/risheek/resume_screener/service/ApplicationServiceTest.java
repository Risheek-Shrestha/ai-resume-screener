package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ApplicationRequest;
import com.risheek.resume_screener.dto.ApplicationResponse;
import com.risheek.resume_screener.entity.*;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ScoreRepository scoreRepository;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {

        applicationService = new ApplicationService(applicationRepository, resumeRepository, jobRepository, scoreRepository, userRepository);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", null));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testApplyForJob_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            applicationService.applyForJob(1L, null);
        });

        assertEquals("Authenticated user not found", exception.getMessage());
    }

    @Test
    void testApplyForJob_JobNotFound() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(user));
        when(jobRepository.findById(anyLong())).thenReturn(java.util.Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            applicationService.applyForJob(1L, null);
        });

        assertEquals("Job not found", exception.getMessage());
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
        resume.setJob(job);

        Score score = new Score();
        score.setId(50L);
        score.setResume(resume);
        score.setOverallScore(BigDecimal.valueOf(71));

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        when(resumeRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(resume));

        when(applicationRepository.existsByUserIdAndJobId(1L,10L))
                .thenReturn(false);

        when(scoreRepository.findByResumeId(100L))
                .thenReturn(Optional.of(score));

        applicationService.applyForJob(10L, request);

        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void testApplyForJob_ResumeNotFound() {

        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.of(job));

        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));

        assertEquals("Resume not found or does not belong to user",
                exception.getMessage());
    }

    @Test
    void testApplyForJob_ResumeForDifferentJob() {

        User user = new User();
        user.setId(1L);

        Job appliedJob = new Job();
        appliedJob.setId(10L);

        Job resumeJob = new Job();
        resumeJob.setId(20L);

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setJob(resumeJob);

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.of(appliedJob));

        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(resume));

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));

        assertEquals("Resume does not belong to the specified job",
                exception.getMessage());
    }

    @Test
    void testApplyForJob_NotStarted() {

        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().plusDays(2));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(10));

        Resume resume = new Resume();
        resume.setJob(job);

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.of(job));

        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(resume));

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));

        assertEquals("Applications are not open for this job",
                exception.getMessage());
    }

    @Test
    void testApplyForJob_Closed() {

        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(10));
        job.setApplicationDeadline(LocalDateTime.now().minusDays(1));

        Resume resume = new Resume();
        resume.setJob(job);

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.of(job));

        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(resume));

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));

        assertEquals("Applications are closed for this job",
                exception.getMessage());
    }

    @Test
    void testApplyForJob_DuplicateApplication() {

        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));

        Resume resume = new Resume();
        resume.setJob(job);

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.of(job));

        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(resume));

        when(applicationRepository.existsByUserIdAndJobId(anyLong(), anyLong()))
                .thenReturn(true);

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));

        assertEquals("User has already applied for this job",
                exception.getMessage());
    }

    @Test
    void testApplyForJob_ScoreNotFound() {

        User user = new User();
        user.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(1));

        Resume resume = new Resume();
        resume.setId(100L);
        resume.setJob(job);

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.of(job));

        when(resumeRepository.findByIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(resume));

        when(applicationRepository.existsByUserIdAndJobId(anyLong(), anyLong()))
                .thenReturn(false);

        when(scoreRepository.findByResumeId(anyLong()))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(10L, request));

        assertEquals("Score not found for the resume",
                exception.getMessage());
    }

    @Test
    void testGetMyApplications_UserNotFound() {

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.getMyApplications());

        assertEquals("Authenticated user not found",
                exception.getMessage());
    }

    @Test
    void testGetMyApplications_Success() {

        User user = new User();
        user.setId(1L);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(applicationRepository.findByUserId(1L))
                .thenReturn(List.of());

        List<ApplicationResponse> responses =
                applicationService.getMyApplications();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testGetApplicationsForJob_Unauthorized() {

        User owner = new User();
        owner.setId(5L);

        User current = new User();
        current.setId(1L);

        Job job = new Job();
        job.setUser(owner);

        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.of(job));

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(current));

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsForJob(1L));

        assertEquals(
                "You are not allowed to view applications for this job",
                exception.getMessage());
    }

    @Test
    void testGetApplicationsForJob_Success() {

        User employer = new User();
        employer.setId(1L);

        Job job = new Job();
        job.setId(10L);
        job.setUser(employer);

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(employer));

        when(applicationRepository.findByJobIdOrderByScoreOverallScoreDesc(10L, BigDecimal.valueOf(50)))
                .thenReturn(List.of());

        List<ApplicationResponse> responses =
                applicationService.getApplicationsForJob(10L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testGetApplicationsForJob_JobNotFound() {

        when(jobRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsForJob(1L));

        assertEquals("Job not found", exception.getMessage());

        verify(userRepository, never()).findByEmail(anyString());
        verify(applicationRepository, never())
                .findByJobIdOrderByScoreOverallScoreDesc(anyLong(), any(BigDecimal.class));
    }

}