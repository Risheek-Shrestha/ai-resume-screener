package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.EducationRequest;
import com.risheek.resume_screener.dto.EducationResponse;
import com.risheek.resume_screener.service.EducationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/educations")
public class EducationController {

    private final EducationService educationService;

    public EducationController(EducationService educationService){
        this.educationService = educationService;
    }

    @GetMapping
    public ResponseEntity<List<EducationResponse>> getAllEducations() {
        List<EducationResponse> educations = educationService.getAllEducations();
        return ResponseEntity.ok(educations);
    }

    @PostMapping
    public ResponseEntity<EducationResponse> addEducation(@Valid @RequestBody EducationRequest request) {
        return ResponseEntity.status(201).body(educationService.addEducation(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EducationResponse> updateEducation(@PathVariable Long id, @Valid @RequestBody EducationRequest request) {
        return ResponseEntity.ok(educationService.updateEducation(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEducation(@PathVariable Long id) {
        educationService.deleteEducation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EducationResponse> getEducation(@PathVariable Long id) {
        return ResponseEntity.ok(educationService.getEducation(id));
    }

}
