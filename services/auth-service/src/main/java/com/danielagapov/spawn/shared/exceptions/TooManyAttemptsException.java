package com.danielagapov.spawn.shared.exceptions;

public class TooManyAttemptsException extends RuntimeException{
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
