package com.risheek.resume_screener.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SkillNormalizerTest {

    @Test
    void normalize_nullInput_returnsEmptyString() {
        assertThat(SkillNormalizer.normalize(null)).isEqualTo("");
    }

    @Test
    void normalize_emptyString_returnsEmptyString() {
        assertThat(SkillNormalizer.normalize("")).isEqualTo("");
    }

    @Test
    void normalize_unmappedSkill_returnsLowercaseTrimmed() {
        assertThat(SkillNormalizer.normalize("  Python  ")).isEqualTo("python");
        assertThat(SkillNormalizer.normalize("Java")).isEqualTo("java");
    }

    @ParameterizedTest(name = "input=''{0}'' → expected=''{1}''")
    @CsvSource({
            "jpa,              hibernate",
            "spring data jpa,  hibernate",
            "postgres,         postgresql",
            "postgres database,postgresql",
            "js,               javascript",
            "statistical analysis, statistics",
            "machine learning, ml",
            "deep learning,    dl",
            "docker containers,docker",
            "k8s,              kubernetes"
    })
    void normalize_mappedAlias_returnsCanonicalSkill(String input, String expected) {
        assertThat(SkillNormalizer.normalize(input.trim())).isEqualTo(expected.trim());
    }

    @Test
    void normalize_mappedAliasWithExtraWhitespace_returnsCanonicalSkill() {
        assertThat(SkillNormalizer.normalize("  jpa  ")).isEqualTo("hibernate");
    }

    @Test
    void normalize_mappedAliasInMixedCase_returnsCanonicalSkill() {
        assertThat(SkillNormalizer.normalize("K8S")).isEqualTo("kubernetes");
        assertThat(SkillNormalizer.normalize("Machine Learning")).isEqualTo("ml");
    }

    @Test
    void normalize_alreadyCanonicalForm_returnsItself() {
        // e.g. "kubernetes" is not itself in the map (only "k8s" maps to it),
        // so it should just come back as "kubernetes" after lowercase/trim
        assertThat(SkillNormalizer.normalize("Kubernetes")).isEqualTo("kubernetes");
        assertThat(SkillNormalizer.normalize("PostgreSQL")).isEqualTo("postgresql");
    }
}