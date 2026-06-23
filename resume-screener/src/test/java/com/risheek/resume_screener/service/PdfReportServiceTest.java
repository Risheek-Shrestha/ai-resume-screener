package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ReportResponse;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

    @Mock
    private ReportService reportService;

    private PdfReportService pdfReportService;

    private ReportResponse buildFullReport() {
        return new ReportResponse(
                1L,
                "Backend Developer",
                new BigDecimal("85"),
                "Strong Match",
                List.of("Java", "Spring Boot"),
                List.of("Kubernetes", "AWS"),
                List.of("Add a project demonstrating cloud deployment"),
                "Ready to Apply"
        );
    }

    private String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    @Test
    void generatePdf_happyPath_returnsNonEmptyByteArray() {
        pdfReportService = new PdfReportService(reportService);

        when(reportService.generateReport(1L)).thenReturn(buildFullReport());

        byte[] result = pdfReportService.generatePdf(1L);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    void generatePdf_textContainsExpectedSections() throws IOException {
        pdfReportService = new PdfReportService(reportService);

        ReportResponse report = buildFullReport();
        when(reportService.generateReport(1L)).thenReturn(report);

        byte[] result = pdfReportService.generatePdf(1L);
        String text = extractText(result);

        assertThat(text).contains(report.getJobTitle());
        assertThat(text).contains(String.valueOf(report.getOverallScore()));
        assertThat(text).contains("Strengths:");
        assertThat(text).contains("Skill Gaps:");
        assertThat(text).contains("Recommendations:");

        for (String strength : report.getStrengths()) {
            assertThat(text).contains(strength);
        }
        for (String improvement : report.getImprovements()) {
            assertThat(text).contains(improvement);
        }
    }

    @Test
    void generatePdf_emptyGapsList_writesNoneLine() throws IOException {
        pdfReportService = new PdfReportService(reportService);

        ReportResponse report = new ReportResponse(
                1L,
                "Backend Developer",
                new BigDecimal("85"),
                "Strong Match",
                List.of("Java", "Spring Boot"),
                Collections.emptyList(),
                List.of("Add a project demonstrating cloud deployment"),
                "Ready to Apply"
        );
        when(reportService.generateReport(1L)).thenReturn(report);

        byte[] result = pdfReportService.generatePdf(1L);
        String text = extractText(result);

        assertThat(text).contains("- None");
    }

    @Test
    void generatePdf_reportServiceThrows_propagatesExceptionUnchanged() {
        pdfReportService = new PdfReportService(reportService);

        when(reportService.generateReport(1L)).thenThrow(new ResumeNotFoundException("Resume not found"));

        assertThrows(ResumeNotFoundException.class,
                () -> pdfReportService.generatePdf(1L));
    }
}