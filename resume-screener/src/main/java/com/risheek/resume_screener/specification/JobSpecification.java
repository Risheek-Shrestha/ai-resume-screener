package com.risheek.resume_screener.specification;

import com.risheek.resume_screener.entity.Application;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public class JobSpecification {

    public static Specification<Job> hasKeyword(String keyword) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Job> hasExperienceLevel(Job.ExperienceLevel level) {
        return (root, query, cb) ->
                cb.equal(root.get("experienceLevel"), level);
    }

    public static Specification<Job> hasJobType(Job.JobType jobType) {
        return (root, query, cb) ->
                cb.equal(root.get("jobType"), jobType);
    }

    public static Specification<Job> hasSkill(String skill) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<JobSkill> jobSkillRoot = subquery.from(JobSkill.class);
            subquery.select(jobSkillRoot.get("job").get("id"))
                    .where(cb.like(cb.lower(jobSkillRoot.get("skillName")), "%" + skill.toLowerCase() + "%"));
            return root.get("id").in(subquery);
        };
    }

    public static Specification<Job> isOpenNow() {
        return (root, query, cb) -> cb.and(
                cb.lessThanOrEqualTo(root.get("applicationStartsAt"), java.time.LocalDateTime.now()),
                cb.greaterThanOrEqualTo(root.get("applicationDeadline"), java.time.LocalDateTime.now())
        );
    }

    // Note: no longer used by JobService#getOpenJobsForUser - that endpoint now
    // shows all open jobs (including ones the user already applied to) along
    // with a per-job userApplicationStatus, rather than hiding them outright.
    // Kept here as a reusable, independently-tested building block.
    public static Specification<Job> notAppliedByUser(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Application> applicationRoot = subquery.from(Application.class);
            subquery.select(applicationRoot.get("job").get("id"))
                    .where(cb.equal(applicationRoot.get("user").get("id"), userId));
            return cb.not(root.get("id").in(subquery));
        };
    }

    public static Specification<Job> buildSpecification(String keyword, Job.JobType jobType,
                                                 Job.ExperienceLevel level, String skill) {
        Specification<Job> spec = (root, query, cb) -> cb.conjunction();
        if (keyword != null && !keyword.isBlank()) spec = spec.and(hasKeyword(keyword));
        if (jobType != null) spec = spec.and(hasJobType(jobType));
        if (level != null) spec = spec.and(hasExperienceLevel(level));
        if (skill != null && !skill.isBlank()) spec = spec.and(hasSkill(skill));
        return spec;
    }
}
