package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Exceptions.ApplicationException;

public class ActivityTypeValidationException extends ApplicationException {
    public ActivityTypeValidationException(String message) {
        super(message);
    }
} 