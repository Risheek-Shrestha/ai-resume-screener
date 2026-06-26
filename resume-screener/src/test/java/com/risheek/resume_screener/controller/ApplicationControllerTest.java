package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.ApplicationRequest;
import com.risheek.resume_screener.dto.ApplicationResponse;
import com.risheek.resume_screener.entity.ApplicationStatus;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.ApplicationService;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.dto.ApplicationResultResponse;
import com.risheek.resume_screener.dto.SuggestionResponse;
import com.risheek.resume_screener.entity.Application;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.Score;
import com.risheek.resume_screener.service.SuggestionService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    @MockitoBean
    private SuggestionService suggestionService;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private ApplicationService applicationService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private CustomUserDetailService customUserDetailService;

    private ApplicationResponse sampleResponse() {
        return new ApplicationResponse(
                1L, 10L, "Backend Developer", 100L,
                BigDecimal.valueOf(92.5), ApplicationStatus.APPLIED, LocalDateTime.now());
    }

    @Test
    @WithMockUser
    void applyForJob_validRequest_returns201() throws Exception {

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        Job job = new Job();
        job.setId(1L);

        Score score = new Score();
        score.setOverallScore(BigDecimal.valueOf(92.5));

        Application application = new Application();
        application.setId(5L);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setScore(score);

        SuggestionResponse suggestionResponse = new SuggestionResponse(
                100L,
                BigDecimal.valueOf(92.5),
                "EXCELLENT",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        when(applicationService.applyForJob(eq(1L), any(ApplicationRequest.class)))
                .thenReturn(application);

        when(suggestionService.getImprovementSuggestions(100L))
                .thenReturn(suggestionResponse);

        mockMvc.perform(post("/api/v1/applications/jobs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(5))
                .andExpect(jsonPath("$.jobId").value(1))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.score").value(92.5))
                .andExpect(jsonPath("$.message")
                        .value("Your profile matches this role — application submitted."));

        verify(applicationService).applyForJob(eq(1L), any(ApplicationRequest.class));
        verify(suggestionService).getImprovementSuggestions(100L);
    }

    @Test
    @WithMockUser
    void getMyApplications_returns200WithList() throws Exception {
        when(applicationService.getMyApplications()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/applications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(1))
                .andExpect(jsonPath("$[0].jobId").value(10))
                .andExpect(jsonPath("$[0].jobTitle").value("Backend Developer"))
                .andExpect(jsonPath("$[0].resumeId").value(100))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));

        verify(applicationService).getMyApplications();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getApplicationsForJob_returns200WithList() throws Exception {
        when(applicationService.getApplicationsForJob(10L)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/applications/jobs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(1))
                .andExpect(jsonPath("$[0].jobId").value(10))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));

        verify(applicationService).getApplicationsForJob(10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAcceptedApplicationsForJob_returns200WithList() throws Exception {
        ApplicationResponse accepted = new ApplicationResponse(
                2L, 10L, "Backend Developer", 101L,
                BigDecimal.valueOf(88.0), ApplicationStatus.APPLIED, LocalDateTime.now());

        when(applicationService.getAcceptedApplicationsForJob(10L)).thenReturn(List.of(accepted));

        mockMvc.perform(get("/api/v1/applications/jobs/10/accepted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(2))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));

        verify(applicationService).getAcceptedApplicationsForJob(10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAcceptedApplicationsForJob_jobNotFound_returns404() throws Exception {
        when(applicationService.getAcceptedApplicationsForJob(999L))
                .thenThrow(new JobNotFoundException("Job not found"));

        mockMvc.perform(get("/api/v1/applications/jobs/999/accepted"))
                .andExpect(status().isNotFound());
    }
}