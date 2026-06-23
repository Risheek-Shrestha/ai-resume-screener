package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.ReportResponse;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.PdfReportService;
import com.risheek.resume_screener.service.ReportService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private PdfReportService pdfReportService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser
    void getReport_existingResumeId_returns200() throws Exception {
        ReportResponse response = new ReportResponse(
                1L, "Strong match", BigDecimal.valueOf(82.5), "HIGH",
                List.of("Strong Java skills"), List.of("No cloud experience"),
                List.of("Learn AWS"), "READY");

        when(reportService.generateReport(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/resume/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeId").value(1));
    }

    @Test
    @WithMockUser
    void getReport_nonExistingResumeId_returns404() throws Exception {

        when(reportService.generateReport(999L))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(get("/api/v1/reports/resume/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getPdfReport_existingResumeId_returns200WithPdfContentTypeAndHeader()
            throws Exception {

        byte[] pdfBytes = "dummy pdf".getBytes();

        when(pdfReportService.generatePdf(1L))
                .thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/reports/resume/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser
    void getPdfReport_nonExistingResumeId_returns404() throws Exception {

        when(pdfReportService.generatePdf(999L))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(get("/api/v1/reports/resume/999/pdf"))
                .andExpect(status().isNotFound());
    }
}