package com.danielagapov.spawn.shared.exceptions;

public class EmailVerificationException extends RuntimeException {
    public EmailVerificationException(String message) {
        super(message);
    }
} 