package com.danielagapov.spawn.Exceptions.Base;

import com.danielagapov.spawn.Enums.EntityType;

public class BasesNotFoundException extends RuntimeException {
    public BasesNotFoundException(EntityType type) {
        super(type + "'s not found.");
    }
}