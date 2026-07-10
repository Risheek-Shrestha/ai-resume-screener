package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.ScoreRequest;
import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoreController.class)
@Import(SecurityConfig.class)
class ScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScoreService scoreService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser
    void generateScore_valid_returns200WithScore() throws Exception {
        ScoreRequest request = new ScoreRequest(1L, 10L);

        ScoreResponse response = new ScoreResponse(
                1L, 100L, 10L, 1L,
                BigDecimal.valueOf(88.0), "Java,Spring", "AWS", "Add cloud projects"
        );

        when(scoreService.generateScore(1L, 10L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/scores")
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.overallScore").value(88.0));

        verify(scoreService).generateScore(1L, 10L);
    }

    @Test
    @WithMockUser
    void generateScore_resumeNotFound_returns404() throws Exception {
        ScoreRequest request = new ScoreRequest(999L, 10L);

        when(scoreService.generateScore(999L, 10L))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(post("/api/v1/scores")
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void generateScore_jobNotFound_returns404() throws Exception {
        ScoreRequest request = new ScoreRequest(1L, 999L);

        when(scoreService.generateScore(1L, 999L))
                .thenThrow(new JobNotFoundException("Job not found"));

        mockMvc.perform(post("/api/v1/scores")
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void generateScore_resumeNotOwnedByUser_returns403() throws Exception {
        ScoreRequest request = new ScoreRequest(1L, 10L);

        when(scoreService.generateScore(1L, 10L))
                .thenThrow(new UnauthorizedAccessException("You are not allowed to access this resume"));

        mockMvc.perform(post("/api/v1/scores")
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void generateScore_unauthenticated_isForbidden() throws Exception {
        ScoreRequest request = new ScoreRequest(1L, 10L);

        mockMvc.perform(post("/api/v1/scores")
                        .with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getMyScores_returnsListOf200() throws Exception {
        ScoreResponse s1 = new ScoreResponse(
                1L, 100L, 10L, 1L,
                BigDecimal.valueOf(88.0), "Java,Spring", "AWS", "Add cloud projects"
        );

        when(scoreService.getMyScores()).thenReturn(List.of(s1));

        mockMvc.perform(get("/api/v1/scores/my-scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(scoreService).getMyScores();
    }

    @Test
    @WithMockUser
    void getScoreByResume_nonExisting_returns404() throws Exception {
        when(scoreService.getScoreByResume(999L, 10L))
                .thenThrow(new ScoreNotFoundException("Score not found for resume id: 999"));

        mockMvc.perform(get("/api/v1/scores/resume/999/job/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getScoreByResume_existing_returns200() throws Exception {
        ScoreResponse response = new ScoreResponse(
                1L, 100L, 10L, 1L,
                BigDecimal.valueOf(88.0), "Java,Spring", "AWS", "Add cloud projects"
        );

        when(scoreService.getScoreByResume(1L, 10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/scores/resume/1/job/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.overallScore").value(88.0));

        verify(scoreService).getScoreByResume(1L, 10L);
    }
}