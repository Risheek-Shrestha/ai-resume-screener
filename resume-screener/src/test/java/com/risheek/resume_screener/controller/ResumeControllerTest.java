package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.ResumeRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ResumeService resumeService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser
    void getMyResumes_returnsListOf200() throws Exception {
        ResumeResponse r1 = new ResumeResponse(1L, 10L, "resume1.pdf", "pdf");
        ResumeResponse r2 = new ResumeResponse(2L, 11L, "resume2.pdf", "pdf");

        when(resumeService.getMyResumes()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser
    void getResumeById_existingId_returns200() throws Exception {

        ResumeResponse response =
                new ResumeResponse(
                        1L,
                        10L,
                        "resume.pdf",
                        "pdf"
                );

        when(resumeService.getResumeById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/resumes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName")
                        .value("resume.pdf"));
    }

    @Test
    @WithMockUser
    void getResumeById_nonExistingId_returns404() throws Exception {

        when(resumeService.getResumeById(999L))
                .thenThrow(
                        new ResumeNotFoundException(
                                "Resume not found"
                        ));

        mockMvc.perform(get("/api/v1/resumes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void uploadResume_validRequest_returns201() throws Exception {

        ResumeRequest request = new ResumeRequest();
        request.setFileName("resume.pdf");
        request.setFileType("pdf");
        request.setFileData("test".getBytes());
        request.setJobId(10L);

        ResumeResponse response =
                new ResumeResponse(1L, 10L, "resume.pdf", "pdf");

        when(resumeService.uploadResume(any(ResumeRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/resumes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(resumeService).uploadResume(any(ResumeRequest.class));
    }

    @Test
    @WithMockUser
    void uploadResume_invalidRequest_returns400() throws Exception {

        ResumeRequest request = new ResumeRequest();
        request.setFileName("");
        request.setFileType("");
        request.setFileData(null);

        mockMvc.perform(post("/api/v1/resumes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resumeService, never())
                .uploadResume(any());
    }

    @Test
    @WithMockUser
    void updateResume_validRequest_returns200() throws Exception {

        ResumeRequest request = new ResumeRequest();
        request.setFileName("updated.pdf");
        request.setFileType("pdf");
        request.setFileData("data".getBytes());
        request.setJobId(10L);

        ResumeResponse response =
                new ResumeResponse(1L, 10L, "updated.pdf", "pdf");

        when(resumeService.updateResume(eq(1L), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/resumes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName")
                        .value("updated.pdf"));
    }

    @Test
    @WithMockUser
    void updateResume_nonExistingId_returns404() throws Exception {

        ResumeRequest request = new ResumeRequest();
        request.setFileName("resume.pdf");
        request.setFileType("pdf");
        request.setFileData("data".getBytes());
        request.setJobId(10L);

        when(resumeService.updateResume(eq(999L), any()))
                .thenThrow(
                        new ResumeNotFoundException(
                                "Resume not found"
                        ));

        mockMvc.perform(put("/api/v1/resumes/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateResume_invalidRequest_returns400() throws Exception {

        ResumeRequest request = new ResumeRequest();
        request.setFileName("");
        request.setFileType("");
        request.setFileData(null);

        mockMvc.perform(put("/api/v1/resumes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resumeService, never())
                .updateResume(anyLong(), any());
    }

    @Test
    @WithMockUser
    void deleteResume_existingId_returns204() throws Exception {

        doNothing().when(resumeService).deleteResume(1L);

        mockMvc.perform(delete("/api/v1/resumes/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(resumeService).deleteResume(1L);
    }

    @Test
    @WithMockUser
    void deleteResume_nonExistingId_returns404() throws Exception {

        doThrow(new ResumeNotFoundException("Resume not found"))
                .when(resumeService)
                .deleteResume(999L);

        mockMvc.perform(delete("/api/v1/resumes/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}