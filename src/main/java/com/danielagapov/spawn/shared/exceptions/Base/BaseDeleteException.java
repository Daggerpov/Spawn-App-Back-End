package com.danielagapov.spawn.shared.exceptions;

public class BaseDeleteException extends RuntimeException {
    public BaseDeleteException(String message) {
        super("Could not delete entity: " + message);
    }
    public BaseDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
