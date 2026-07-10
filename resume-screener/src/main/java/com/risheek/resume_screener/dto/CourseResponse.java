package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseResponse {

    private Long id;
    private String name;
    private int totalYears;

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getTotalYears()
        );
    }
}