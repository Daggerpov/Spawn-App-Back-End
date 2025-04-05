package com.danielagapov.spawn.Exceptions.Base;

import com.danielagapov.spawn.Enums.EntityType;

public class BasesNotFoundException extends RuntimeException {
    public final EntityType entityType;
    public BasesNotFoundException(EntityType type) {
        super(type + "'s not found.");
        this.entityType = type;
    }
}