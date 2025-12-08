package com.danielagapov.spawn.shared.exceptions;

public class BaseSaveException extends RuntimeException {
    public BaseSaveException(String message) {
        super("failed to save an entity: " + message);
    }
}