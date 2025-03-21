package com.danielagapov.spawn.Exceptions;

import java.util.UUID;

import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;

public class UserNotFoundException extends BaseNotFoundException {
    public UserNotFoundException(UUID id) {
        super(EntityType.User, id);
    }
}
