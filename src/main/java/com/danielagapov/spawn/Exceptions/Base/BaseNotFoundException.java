package com.danielagapov.spawn.Exceptions.Base;

import com.danielagapov.spawn.Enums.EntityType;

import java.util.UUID;

public class BaseNotFoundException extends RuntimeException {
    public final EntityType et;
    
    public BaseNotFoundException(EntityType et, UUID id) {
        super(et +  " entity not found with ID: " + id);
        this.et = et;
    }
    public BaseNotFoundException(EntityType et, String username) {
        super(et +  " entity not found with username: " + username);
        this.et = et;
    }
}