package com.danielagapov.spawn.Exceptions.Base;

import com.danielagapov.spawn.Enums.EntityType;

import java.util.UUID;

public class BasesNotFoundException extends RuntimeException {
    public BasesNotFoundException(EntityType entityType, UUID id) {
        super(entityType.getDescription() + " not found with UUID: " + id);
    }
    public BasesNotFoundException(String type) {
        super(type + " not found.");
    }
}