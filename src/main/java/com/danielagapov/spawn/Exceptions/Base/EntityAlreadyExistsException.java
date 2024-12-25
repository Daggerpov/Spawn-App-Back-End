package com.danielagapov.spawn.Exceptions.Base;

import com.danielagapov.spawn.Enums.EntityType;

import java.util.UUID;

public class EntityAlreadyExistsException extends RuntimeException {
    public EntityAlreadyExistsException(EntityType entityType, UUID id) {
        super("Entity already exists for type, " + entityType.getDescription() + " with UUID: " + id);
    }
}