package com.danielagapov.spawn.shared.exceptions;

import com.danielagapov.spawn.shared.util.EntityType;

public class BasesNotFoundException extends RuntimeException {
    public final EntityType entityType;
    public BasesNotFoundException(EntityType type) {
        super(type + "'s not found.");
        this.entityType = type;
    }
}