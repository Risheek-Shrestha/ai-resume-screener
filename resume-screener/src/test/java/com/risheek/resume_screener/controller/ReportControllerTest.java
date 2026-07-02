package com.risheek.resume_screener.controller;

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

    @MockitoBean private ReportService reportService;
    @MockitoBean private PdfReportService pdfReportService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser
    void getReport_existing_returns200() throws Exception {
        ReportResponse response = new ReportResponse(
                1L, "Backend Developer", BigDecimal.valueOf(82.5), "EXCELLENT",
                List.of("Strong Java skills"), List.of("No cloud experience"),
                List.of("Learn AWS"), "Interview Ready");

        when(reportService.generateReport(1L, 10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/resume/1/job/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeId").value(1))
                .andExpect(jsonPath("$.jobTitle").value("Backend Developer"));
    }

    @Test
    @WithMockUser
    void getReport_notFound_returns404() throws Exception {
        when(reportService.generateReport(999L, 10L))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(get("/api/v1/reports/resume/999/job/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getPdfReport_existing_returns200WithPdfHeaders() throws Exception {
        when(pdfReportService.generatePdf(1L, 10L)).thenReturn("dummy pdf".getBytes());

        mockMvc.perform(get("/api/v1/reports/resume/1/job/10/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser
    void getPdfReport_notFound_returns404() throws Exception {
        when(pdfReportService.generatePdf(999L, 10L))
                .thenThrow(new ResumeNotFoundException("Resume not found"));

        mockMvc.perform(get("/api/v1/reports/resume/999/job/10/pdf"))
                .andExpect(status().isNotFound());
    }
}
