package com.danielagapov.spawn.Exceptions.Base;

import java.util.UUID;

public class EntityAlreadyExistsException extends RuntimeException {
    public EntityAlreadyExistsException(String type, UUID id) {
        super("Entity already exists for type, " + type + " with UUID: " + id);
    }
}