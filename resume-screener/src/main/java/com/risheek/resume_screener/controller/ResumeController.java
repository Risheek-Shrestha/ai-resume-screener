package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.JobRequest;
import com.risheek.resume_screener.dto.JobResponse;
import com.risheek.resume_screener.dto.ResumeRequest;
import com.risheek.resume_screener.dto.ResumeResponse;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.service.JobService;
import com.risheek.resume_screener.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> uploadResume(@RequestParam("file") MultipartFile file,
                                                       @RequestParam("resumeName") String resumeName) throws IOException {
        return ResponseEntity.status(201).body(resumeService.uploadResume(file, resumeName));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateResume(@PathVariable Long id,@RequestParam("file") MultipartFile file,
                                                       @RequestParam("resumeName") String resumeName ) throws IOException {
        return ResponseEntity.ok(resumeService.updateResume(id, file, resumeName));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id) {
        resumeService.deleteResume(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> getMyResumes() {
        return ResponseEntity.ok(resumeService.getMyResumes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                resumeService.getResumeById(id)
        );
    }

    @GetMapping("/application/{applicationId}/download")
    public ResponseEntity<byte[]> downloadResumeForApplication(
            @PathVariable Long applicationId) {

        ResumeResponse resume =
                resumeService.getResumeForApplication(applicationId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resume.getFileName() + "\""
                )
                .contentType(MediaType.parseMediaType(resume.getFileType()))
                .body(resume.getFileData());
    }

}
