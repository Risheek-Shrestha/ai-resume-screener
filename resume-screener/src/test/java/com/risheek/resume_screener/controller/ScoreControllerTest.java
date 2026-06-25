package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.ScoreResponse;
import com.risheek.resume_screener.exception.ScoreNotFoundException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoreController.class)
@Import(SecurityConfig.class)
class ScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ScoreService scoreService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser
    void getMyScores_returnsListOf200() throws Exception {

        ScoreResponse s1 = new ScoreResponse(
                1L,
                100L,
                10L,
                1L,
                BigDecimal.valueOf(88.0),
                "Java,Spring",
                "AWS",
                "Add cloud projects"
        );

        when(scoreService.getMyScores())
                .thenReturn(List.of(s1));

        mockMvc.perform(get("/api/v1/scores/my-scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(scoreService).getMyScores();
    }

    @Test
    @WithMockUser
    void getScoreByResume_nonExistingResumeId_returns404() throws Exception {

        when(scoreService.getScoreByResume(999L))
                .thenThrow(
                        new ScoreNotFoundException(
                                "Score not found for resume id: 999"
                        ));

        mockMvc.perform(get("/api/v1/scores/resume/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getScoreByResume_existingResumeId_returns200() throws Exception {

        ScoreResponse response = new ScoreResponse(
                1L,
                100L,
                10L,
                1L,
                BigDecimal.valueOf(88.0),
                "Java,Spring",
                "AWS",
                "Add cloud projects"
        );

        when(scoreService.getScoreByResume(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/scores/resume/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.overallScore").value(88.0));

        verify(scoreService).getScoreByResume(1L);
    }
}