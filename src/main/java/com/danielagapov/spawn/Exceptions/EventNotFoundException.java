package com.danielagapov.spawn.Exceptions;

import java.util.UUID;

import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;

public class ActivityNotFoundException extends BaseNotFoundException{
    public ActivityNotFoundException(UUID id) {
        super(EntityType.Activity, id);
    }
}
