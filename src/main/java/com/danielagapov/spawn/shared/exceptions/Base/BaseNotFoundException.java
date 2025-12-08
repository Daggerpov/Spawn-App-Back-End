package com.danielagapov.spawn.shared.exceptions;

import com.danielagapov.spawn.shared.util.EntityType;

import java.util.UUID;

public class BaseNotFoundException extends RuntimeException {
    public final EntityType entityType;
    
    public BaseNotFoundException(EntityType et) {
        super(et +  " entity not found");
        this.entityType = et;
    }

    public BaseNotFoundException(EntityType et, UUID id) {
        super(et +  " entity not found with ID: " + id);
        this.entityType = et;
    }
  
    /**
     *
     * @param et The entity type that could not be found.
     * @param identifier this could be something like a user's email or username
     * @param identifierType to indicate if it's an email, username, or something else in the print statement.
     */
    public BaseNotFoundException(EntityType et, String identifier, String identifierType) {
        super(et +  " entity not found with " + identifierType + ": " + identifier);
        this.entityType = et;
    }
}