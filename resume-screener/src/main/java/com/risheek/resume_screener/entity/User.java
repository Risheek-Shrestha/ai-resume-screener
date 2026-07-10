package com.risheek.resume_screener.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username required")
    @Column(nullable = false)
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email address required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true)
    @Size(min = 3, max = 50)
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Column(nullable = false)
    private String passwordHash;

    public enum Role {
        USER,
        ADMIN
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String phoneNumber;

    private LocalDate dateOfBirth;

    @Column(columnDefinition = "bytea")
    private byte[] profilePhotoData;

    private String profilePhotoType;

    private String currentCollege;

    @ManyToOne
    @JoinColumn(name = "current_course_id")
    private Course currentCourse;

    @Min(value = 1, message = "Semester must be at least 1")
    private Integer currentSemester;
}