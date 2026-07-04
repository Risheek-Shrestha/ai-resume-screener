package com.risheek.resume_screener.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Enter course name")
    @Column(nullable = false, unique = true)
    private String name;

    @Min(value = 1, message = "Course duration must be at least 1 year")
    @Column(nullable = false)
    private int totalYears;
}