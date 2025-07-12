package com.danielagapov.spawn.Exceptions;

public class EmailVerificationException extends RuntimeException {
    public EmailVerificationException(String message) {
        super(message);
    }
} 