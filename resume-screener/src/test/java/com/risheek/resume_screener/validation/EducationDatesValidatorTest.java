package com.risheek.resume_screener.validation;

import com.risheek.resume_screener.dto.EducationRequest;
import com.risheek.resume_screener.entity.EducationLevel;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EducationDatesValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private EducationDatesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EducationDatesValidator();
    }

    private EducationRequest buildRequest(LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
        return new EducationRequest(
                EducationLevel.BACHELORS,
                "Test University",
                startDate,
                endDate,
                "A",
                isCurrent
        );
    }

    @Test
    void isValid_nullValue_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void isValid_nullIsCurrent_returnsTrue() {
        EducationRequest request = buildRequest(LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), null);
        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    void isValid_notCurrentAndMissingEndDate_returnsFalse() {
        EducationRequest request = buildRequest(LocalDate.of(2020, 1, 1), null, false);
        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    void isValid_notCurrentWithEndDateAfterStartDate_returnsTrue() {
        EducationRequest request = buildRequest(LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), false);
        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    void isValid_endDateBeforeStartDate_returnsFalse() {
        EducationRequest request = buildRequest(LocalDate.of(2022, 1, 1), LocalDate.of(2020, 1, 1), false);
        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    void isValid_endDateEqualsStartDate_returnsTrue() {
        LocalDate date = LocalDate.of(2021, 6, 15);
        EducationRequest request = buildRequest(date, date, false);
        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    void isValid_currentWithNoEndDate_returnsTrue() {
        EducationRequest request = buildRequest(LocalDate.of(2020, 1, 1), null, true);
        assertThat(validator.isValid(request, context)).isTrue();
    }

    @Test
    void isValid_currentWithEndDatePresent_returnsFalse() {
        EducationRequest request = buildRequest(LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), true);
        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    void isValid_missingStartDateOnly_returnsTrue() {
        EducationRequest request = buildRequest(null, LocalDate.of(2022, 1, 1), false);
        assertThat(validator.isValid(request, context)).isTrue();
    }
}