package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.JobSuggestionResponse;
import com.risheek.resume_screener.dto.SuggestionResponse;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.SuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuggestionController.class)
@Import(SecurityConfig.class)
class SuggestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SuggestionService suggestionService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser
    void getImprovementSuggestions_existing_returns200() throws Exception {
        SuggestionResponse response = new SuggestionResponse(
                1L, BigDecimal.valueOf(65.0), "MEDIUM",
                List.of("AWS", "Docker"), List.of("Cloud experience"),
                List.of("Build a project using AWS"),
                List.of("AWS Certified Developer course"),
                List.of("Add quantifiable achievements"));

        when(suggestionService.getImprovementSuggestions(1L, 10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/suggestions/improve/1/job/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeId").value(1))
                .andExpect(jsonPath("$.scoreLevel").value("MEDIUM"));
    }

    @Test
    @WithMockUser
    void getImprovementSuggestions_notFound_returns404() throws Exception {
        when(suggestionService.getImprovementSuggestions(999L, 10L))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(get("/api/v1/suggestions/improve/999/job/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getSuggestedJobs_existing_returns200WithList() throws Exception {
        JobSuggestionResponse job = new JobSuggestionResponse(
                1L, "Java Developer", "FULL_TIME", "MID",
                91.0, List.of("Java", "Spring"), List.of("AWS"),
                0.87, "Strong skill overlap");

        when(suggestionService.getSuggestedJobs(1L, 10L)).thenReturn(List.of(job));

        mockMvc.perform(get("/api/v1/suggestions/jobs/1/job/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].jobTitle").value("Java Developer"));
    }

    @Test
    @WithMockUser
    void getSuggestedJobs_notFound_returns404() throws Exception {
        when(suggestionService.getSuggestedJobs(999L, 10L))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(get("/api/v1/suggestions/jobs/999/job/10"))
                .andExpect(status().isNotFound());
    }
}
