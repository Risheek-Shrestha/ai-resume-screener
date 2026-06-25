package com.risheek.resume_screener.exception;

public class DuplicateApplicationException
        extends RuntimeException {

    public DuplicateApplicationException(String message) {
        super(message);
    }
}