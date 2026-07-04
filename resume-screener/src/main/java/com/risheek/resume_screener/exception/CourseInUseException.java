package com.risheek.resume_screener.exception;

public class CourseInUseException extends RuntimeException {
    public CourseInUseException(String message) {
        super(message);
    }
}
