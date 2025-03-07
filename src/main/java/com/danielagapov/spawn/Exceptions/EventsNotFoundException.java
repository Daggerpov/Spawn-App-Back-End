package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Enums.EntityType;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;

public class EventsNotFoundException extends BasesNotFoundException{
    public EventsNotFoundException() {
        super(EntityType.Event);
    }
}
