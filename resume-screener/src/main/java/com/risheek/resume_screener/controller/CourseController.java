package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.CourseRequest;
import com.risheek.resume_screener.dto.CourseResponse;
import com.risheek.resume_screener.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService CourseService;

    public CourseController(CourseService CourseService) {
        this.CourseService = CourseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(CourseService.getAllCourses());
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse (@Valid @RequestBody CourseRequest request){
        return    ResponseEntity.status(201).body(CourseService.createCourse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest request){
        return  ResponseEntity.ok(CourseService.updateCourse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id){
        CourseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
