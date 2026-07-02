package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.ReportResponse;
import com.risheek.resume_screener.service.PdfReportService;
import com.risheek.resume_screener.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final PdfReportService pdfReportService;

    public ReportController(ReportService reportService, PdfReportService pdfReportService) {
        this.reportService = reportService;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping("/resume/{resumeId}/job/{jobId}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable Long resumeId, @PathVariable Long jobId) {
        return ResponseEntity.ok(reportService.generateReport(resumeId, jobId));
    }

    @GetMapping("/resume/{resumeId}/job/{jobId}/pdf")
    public ResponseEntity<byte[]> getPdfReport(
            @PathVariable Long resumeId, @PathVariable Long jobId) {
        byte[] pdf = pdfReportService.generatePdf(resumeId, jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resume-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
