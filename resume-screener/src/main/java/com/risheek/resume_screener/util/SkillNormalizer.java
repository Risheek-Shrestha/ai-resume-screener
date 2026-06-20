package com.risheek.resume_screener.util;

import java.util.HashMap;
import java.util.Map;

public class SkillNormalizer {

    private static final Map<String, String> SKILL_MAP = new HashMap<>();

    static {

        // Java Ecosystem
        SKILL_MAP.put("jpa", "hibernate");
        SKILL_MAP.put("spring data jpa", "hibernate");

        // PostgreSQL
        SKILL_MAP.put("postgres", "postgresql");
        SKILL_MAP.put("postgres database", "postgresql");

        // JavaScript
        SKILL_MAP.put("js", "javascript");

        // Machine Learning
        SKILL_MAP.put("statistical analysis", "statistics");
        SKILL_MAP.put("machine learning", "ml");
        SKILL_MAP.put("deep learning", "dl");

        // Docker
        SKILL_MAP.put("docker containers", "docker");

        // Kubernetes
        SKILL_MAP.put("k8s", "kubernetes");
    }

    public static String normalize(String skill) {

        if (skill == null) {
            return "";
        }

        String normalized =
                skill.trim().toLowerCase();

        return SKILL_MAP.getOrDefault(
                normalized,
                normalized
        );
    }
}