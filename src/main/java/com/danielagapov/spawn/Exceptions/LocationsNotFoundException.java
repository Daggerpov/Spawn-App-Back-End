package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Enums.EntityType;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;

public class LocationsNotFoundException extends BasesNotFoundException{
    public LocationsNotFoundException() {
        super(EntityType.Location);
    }
}
