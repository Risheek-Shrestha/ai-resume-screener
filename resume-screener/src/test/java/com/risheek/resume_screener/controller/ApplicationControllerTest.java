package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.ApplicationRequest;
import com.risheek.resume_screener.dto.ApplicationResponse;
import com.risheek.resume_screener.entity.ApplicationStatus;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.ApplicationService;
import com.risheek.resume_screener.service.CustomUserDetailService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApplicationService applicationService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser
    void testApplyForJob() throws Exception {

        ApplicationRequest request = new ApplicationRequest();
        request.setResumeId(100L);

        doNothing().when(applicationService)
                .applyForJob(eq(1L), any(ApplicationRequest.class));

        mockMvc.perform(post("/api/v1/applications/jobs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(applicationService)
                .applyForJob(eq(1L), any(ApplicationRequest.class));
    }

    @Test
    @WithMockUser
    void testGetMyApplications() throws Exception {

        ApplicationResponse response = new ApplicationResponse(
                1L,
                10L,
                "Backend Developer",
                100L,
                BigDecimal.valueOf(92.5),
                ApplicationStatus.APPLIED,
                LocalDateTime.now()
        );

        when(applicationService.getMyApplications())
                .thenReturn(List.of(response));

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
    void testGetApplicationsForJob() throws Exception {

        ApplicationResponse response = new ApplicationResponse(
                1L,
                10L,
                "Backend Developer",
                100L,
                BigDecimal.valueOf(92.5),
                ApplicationStatus.APPLIED,
                LocalDateTime.now()
        );

        when(applicationService.getApplicationsForJob(10L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/applications/jobs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(1))
                .andExpect(jsonPath("$[0].jobId").value(10))
                .andExpect(jsonPath("$[0].jobTitle").value("Backend Developer"))
                .andExpect(jsonPath("$[0].resumeId").value(100))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));

        verify(applicationService).getApplicationsForJob(10L);
    }
}