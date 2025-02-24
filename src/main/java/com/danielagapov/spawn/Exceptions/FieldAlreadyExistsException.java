package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Enums.UserField;

public class FieldAlreadyExistsException extends RuntimeException {
    protected final UserField field;

    public FieldAlreadyExistsException(String message, UserField field) {
        super(message);
        this.field = field;
    }
}
