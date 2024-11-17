package com.danielagapov.spawn.Exceptions.Base;

public class BaseNotFoundException extends RuntimeException {
    public BaseNotFoundException(Long id) {
        super("Entity not found with ID: " + id);
    }
}