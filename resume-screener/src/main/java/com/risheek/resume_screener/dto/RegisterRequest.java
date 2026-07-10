package com.risheek.resume_screener.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email address required")
    @Email(message = "Invalid email format")
    @Size(min = 3, max = 50)
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Enter phone number")
    private String phoneNumber;

    @NotNull(message = "Enter date of birth")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Enter current college")
    private String currentCollege;

    @NotNull(message = "Select current course")
    private Long currentCourseId;

    @NotNull(message = "Enter current semester")
    @Min(value = 1, message = "Semester must be at least 1")
    private Integer currentSemester;
}