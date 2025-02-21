package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Enums.Field;

public class FieldAlreadyExistsException extends RuntimeException {
    protected final Field field;
    public FieldAlreadyExistsException(String message, Field field) {
        super(message);
        this.field = field;
    }
}
