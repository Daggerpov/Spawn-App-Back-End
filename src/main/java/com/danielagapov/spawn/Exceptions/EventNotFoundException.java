package com.danielagapov.spawn.Exceptions;

import java.util.UUID;

import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;

public class EventNotFoundException extends BaseNotFoundException{
    public EventNotFoundException(UUID id) {
        super(EntityType.Event, id);
    }
}
