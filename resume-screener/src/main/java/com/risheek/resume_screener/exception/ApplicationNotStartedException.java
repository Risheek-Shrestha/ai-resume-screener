package com.risheek.resume_screener.exception;

public class ApplicationNotStartedException
        extends RuntimeException {

    public ApplicationNotStartedException(String message) {
        super(message);
    }
}