package com.danielagapov.spawn.Exceptions.Base;

public class ApplicationException extends RuntimeException {
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
    public ApplicationException(String message) {super(message);}
}
