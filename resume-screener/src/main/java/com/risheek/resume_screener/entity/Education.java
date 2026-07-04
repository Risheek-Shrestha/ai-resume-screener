package com.risheek.resume_screener.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "education")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Enter education level")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EducationLevel level;

    @NotBlank(message = "Enter institution name")
    @Column(nullable = false)
    private String institution;

    @NotNull(message = "Enter start date")
    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @NotBlank(message = "Enter grade")
    @Column(nullable = false)
    private String grade;

    @NotNull(message = "Enter passing status")
    @Column(nullable = false)
    private Boolean isCurrent;
}