package com.risheek.resume_screener.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
// Note: previously had a unique(user_id, job_id) constraint, which blocked a
// user from ever re-applying to the same job after being rejected. Users can
// now submit multiple applications for the same job over time (e.g. after
// being REJECTED, with a new/updated resume); duplicates are instead
// prevented only while an application is still "active" - see
// ApplicationService#applyForJob.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    // ManyToOne (not OneToOne): with re-applies now allowed, ApplicationService
    // reuses an existing Score row when the same resume is scored against the
    // same job again, so more than one Application can legitimately point at
    // the same Score. OneToOne implicitly enforced a unique constraint on
    // score_id, which broke re-applying with the same resume.
    @ManyToOne
    @JoinColumn(name = "score_id")
    private Score score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}