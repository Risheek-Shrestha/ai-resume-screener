package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.EducationRequest;
import com.risheek.resume_screener.dto.EducationResponse;
import com.risheek.resume_screener.entity.EducationLevel;
import com.risheek.resume_screener.exception.EducationNotFound;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.EducationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EducationController.class)
@Import(SecurityConfig.class)
class EducationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EducationService educationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    private EducationResponse sampleResponse() {
        return new EducationResponse(
                1L, 100L, EducationLevel.BACHELORS, "IIT Delhi",
                LocalDate.of(2018, 7, 1), LocalDate.of(2022, 6, 30), "8.5 CGPA", false
        );
    }

    private EducationRequest sampleRequest() {
        return new EducationRequest(
                EducationLevel.BACHELORS, "IIT Delhi",
                LocalDate.of(2018, 7, 1), LocalDate.of(2022, 6, 30), "8.5 CGPA", false
        );
    }

    @Test
    @WithMockUser
    void getAllEducations_returnsListOf200() throws Exception {
        when(educationService.getAllEducations()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/educations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].institution").value("IIT Delhi"));

        verify(educationService).getAllEducations();
    }

    @Test
    @WithMockUser
    void addEducation_valid_returns201() throws Exception {
        when(educationService.addEducation(any(EducationRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/educations")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.institution").value("IIT Delhi"));

        verify(educationService).addEducation(any(EducationRequest.class));
    }

    @Test
    @WithMockUser
    void addEducation_invalid_returns400() throws Exception {
        EducationRequest invalid = new EducationRequest(
                null, "", null, null, "", null
        );

        mockMvc.perform(post("/api/v1/educations")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void updateEducation_existing_returns200() throws Exception {
        when(educationService.updateEducation(eq(1L), any(EducationRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/educations/1")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(educationService).updateEducation(eq(1L), any(EducationRequest.class));
    }

    @Test
    @WithMockUser
    void updateEducation_nonExisting_returns404() throws Exception {
        when(educationService.updateEducation(eq(999L), any(EducationRequest.class)))
                .thenThrow(new EducationNotFound("Education not found with id: 999"));

        mockMvc.perform(put("/api/v1/educations/999")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteEducation_existing_returns204() throws Exception {
        doNothing().when(educationService).deleteEducation(1L);

        mockMvc.perform(delete("/api/v1/educations/1")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());

        verify(educationService).deleteEducation(1L);
    }

    @Test
    @WithMockUser
    void deleteEducation_nonExisting_returns404() throws Exception {
        doThrow(new EducationNotFound("Education not found with id: 999"))
                .when(educationService).deleteEducation(999L);

        mockMvc.perform(delete("/api/v1/educations/999")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }
}