package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ReportResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

@Service
public class PdfReportService {

    private final ReportService reportService;

    public PdfReportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public byte[] generatePdf(Long resumeId) {

        ReportResponse report =
                reportService.generateReport(resumeId);

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream content =
                    new PDPageContentStream(document, page);

            content.setFont(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                    18
            );
            content.beginText();
            content.newLineAtOffset(50, 750);
            content.showText("AI Resume Analysis Report");
            content.endText();

            content.setFont(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    12
            );

            int y = 700;

            y = writeLine(content, "Job Title: " + report.getJobTitle(), y);
            y = writeLine(content, "Overall Score: " + report.getOverallScore() + "%", y);
            y = writeLine(content, "Score Level: " + report.getScoreLevel(), y);
            y = writeLine(content, "Job Readiness: " + report.getJobReadiness(), y);

            y -= 20;

            y = writeLine(content, "Strengths:", y);

            for (String skill : report.getStrengths()) {
                y = writeLine(content, " - " + skill, y);
            }

            y -= 10;

            y = writeLine(content, "Skill Gaps:", y);

            if (report.getGaps().isEmpty()) {
                y = writeLine(content, " - None", y);
            } else {
                for (String skill : report.getGaps()) {
                    y = writeLine(content, " - " + skill, y);
                }
            }

            y -= 10;

            y = writeLine(content, "Recommendations:", y);

            for (String recommendation : report.getImprovements()) {
                y = writeLine(content, " - " + recommendation, y);
            }

            content.close();

            document.save(out);

            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private int writeLine(
            PDPageContentStream content,
            String text,
            int y
    ) throws IOException {

        content.beginText();
        content.newLineAtOffset(50, y);
        content.showText(text);
        content.endText();

        return y - 20;
    }

}
