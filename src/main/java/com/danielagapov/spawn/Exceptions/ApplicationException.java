package com.danielagapov.spawn.Exceptions;

public class ApplicationException extends RuntimeException {
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
