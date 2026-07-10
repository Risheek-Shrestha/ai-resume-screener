package com.risheek.resume_screener.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EducationDatesValidator.class)
public @interface ValidEducationDates {

    String message() default "endDate is required when isCurrent is false, and must not be before startDate";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
