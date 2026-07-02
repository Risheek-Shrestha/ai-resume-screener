package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.ResumeResponse;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
@Import(SecurityConfig.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeService resumeService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser
    void getMyResumes_returnsListOf200() throws Exception {
        ResumeResponse r1 = new ResumeResponse(1L, "Backend Dev Resume", "resume1.pdf", "pdf");
        ResumeResponse r2 = new ResumeResponse(2L, "Backend Dev Resume", "resume2.pdf", "pdf");

        when(resumeService.getMyResumes()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser
    void getResumeById_existingId_returns200() throws Exception {
        ResumeResponse response = new ResumeResponse(1L, "Backend Dev Resume", "resume.pdf", "pdf");

        when(resumeService.getResumeById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/resumes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("resume.pdf"));
    }

    @Test
    @WithMockUser
    void getResumeById_nonExistingId_returns404() throws Exception {
        when(resumeService.getResumeById(999L))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(get("/api/v1/resumes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void uploadResume_validRequest_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes());

        ResumeResponse response = new ResumeResponse(1L, "Backend Dev Resume", "resume.pdf", "application/pdf");

        when(resumeService.uploadResume(any(), anyString())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(file)
                        .param("resumeName", "Backend Dev Resume")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(resumeService).uploadResume(any(), eq("Backend Dev Resume"));
    }

    @Test
    @WithMockUser
    void uploadResume_missingParam_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(resumeService, never()).uploadResume(any(), any());
    }

    @Test
    @WithMockUser
    void updateResume_validRequest_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "updated.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes());

        ResumeResponse response = new ResumeResponse(1L, "Updated Resume", "updated.pdf", "application/pdf");

        when(resumeService.updateResume(eq(1L), any(), anyString())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/resumes/1")
                        .file(file)
                        .param("resumeName", "Updated Resume")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("updated.pdf"));
    }

    @Test
    @WithMockUser
    void updateResume_nonExistingId_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "content".getBytes());

        when(resumeService.updateResume(eq(999L), any(), anyString()))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(multipart("/api/v1/resumes/999")
                        .file(file)
                        .param("resumeName", "Resume")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteResume_existingId_returns204() throws Exception {
        doNothing().when(resumeService).deleteResume(1L);

        mockMvc.perform(delete("/api/v1/resumes/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(resumeService).deleteResume(1L);
    }

    @Test
    @WithMockUser
    void deleteResume_nonExistingId_returns404() throws Exception {
        doThrow(new ResumeNotFoundException("Resume not found"))
                .when(resumeService).deleteResume(999L);

        mockMvc.perform(delete("/api/v1/resumes/999").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
