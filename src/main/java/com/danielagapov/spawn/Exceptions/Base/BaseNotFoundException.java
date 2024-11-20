package com.danielagapov.spawn.Exceptions.Base;

import java.util.UUID;

public class BaseNotFoundException extends RuntimeException {
    public BaseNotFoundException(UUID id) {
        super("Entity not found with ID: " + id);
    }
}