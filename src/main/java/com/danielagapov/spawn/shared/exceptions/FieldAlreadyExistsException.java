package com.danielagapov.spawn.shared.exceptions;

import com.danielagapov.spawn.shared.util.UserField;

public class FieldAlreadyExistsException extends RuntimeException {
    protected final UserField field;

    public FieldAlreadyExistsException(String message, UserField field) {
        super(message);
        this.field = field;
    }
}
