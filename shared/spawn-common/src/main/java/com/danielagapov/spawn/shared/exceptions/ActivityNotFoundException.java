package com.danielagapov.spawn.shared.exceptions;

import java.util.UUID;

import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;

public class ActivityNotFoundException extends BaseNotFoundException{
    public ActivityNotFoundException(UUID id) {
        super(EntityType.Activity, id);
    }
}
