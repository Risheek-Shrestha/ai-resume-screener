package com.risheek.resume_screener.validation;

import com.risheek.resume_screener.dto.EducationRequest;
import jakarta.validation.ConstraintValidator;

public class EducationDatesValidator implements ConstraintValidator<ValidEducationDates, EducationRequest> {

    @Override
    public boolean isValid(EducationRequest value, jakarta.validation.ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        Boolean isCurrent = value.getIsCurrent();
        if (isCurrent == null) {
            return true;
        }

        if (isCurrent) {
            return value.getEndDate() == null;
        }

        if (value.getEndDate() == null) {
            return false;
        }
        if (value.getStartDate() == null) {
            return true;
        }
        return !value.getEndDate().isBefore(value.getStartDate());
    }
}
