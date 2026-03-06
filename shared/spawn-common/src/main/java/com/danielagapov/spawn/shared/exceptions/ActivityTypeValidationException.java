package com.danielagapov.spawn.shared.exceptions;

import com.danielagapov.spawn.shared.exceptions.ApplicationException;

public class ActivityTypeValidationException extends ApplicationException {
    public ActivityTypeValidationException(String message) {
        super(message);
    }
} 