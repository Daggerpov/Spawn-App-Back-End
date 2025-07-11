package com.danielagapov.spawn.Exceptions;

public class TooManyAttemptsException extends RuntimeException{
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
