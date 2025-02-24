package com.danielagapov.spawn.Exceptions;

import static com.danielagapov.spawn.Enums.UserField.EMAIL;

public class EmailAlreadyExistsException extends FieldAlreadyExistsException {
    public EmailAlreadyExistsException(String message) {
        super(message, EMAIL);
    }
}
