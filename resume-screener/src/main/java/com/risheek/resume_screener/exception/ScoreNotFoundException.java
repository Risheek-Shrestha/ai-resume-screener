package com.risheek.resume_screener.exception;

public class ScoreNotFoundException
        extends RuntimeException {

    public ScoreNotFoundException(String message) {
        super(message);
    }
}