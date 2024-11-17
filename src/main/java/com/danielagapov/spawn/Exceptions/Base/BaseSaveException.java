package com.danielagapov.spawn.Exceptions.Base;

public class BaseSaveException extends RuntimeException {
    public BaseSaveException(String message) {
        super("failed to save an entity: " + message);
    }
}