package com.risheek.resume_screener.exception;

public class ApplicationClosedException
        extends RuntimeException {

    public ApplicationClosedException(String message) {
        super(message);
    }
}