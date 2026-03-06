package com.danielagapov.spawn.shared.exceptions;

import com.danielagapov.spawn.shared.util.EntityType;

import java.util.UUID;

public class EntityAlreadyExistsException extends RuntimeException {
    public EntityAlreadyExistsException(EntityType entityType, UUID id) {
        super("Entity already exists for type, " + entityType.getDescription() + " with UUID: " + id);
    }
}