package com.risheek.resume_screener.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Job title required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Enter job description")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    public enum JobType {
        FULL_TIME,
        PART_TIME,
        CONTRACT,
        INTERNSHIP
    }

    @NotNull(message = "Please mention job type")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType jobType;

    public enum ExperienceLevel {
        JUNIOR,
        MID,
        SENIOR
    }

    @NotNull(message = "Enter experience level")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperienceLevel experienceLevel;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime applicationStartsAt;

    @Column(nullable = false)
    private LocalDateTime applicationDeadline;

    @Column(nullable = false)
    private boolean openNotificationSent = false;

    @ManyToMany
    @JoinTable(
            name = "job_eligible_courses",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> eligibleCourses = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "job_eligible_semesters",
            joinColumns = @JoinColumn(name = "job_id")
    )
    @Column(name = "semester")
    private Set<Integer> eligibleSemesters = new HashSet<>();
}