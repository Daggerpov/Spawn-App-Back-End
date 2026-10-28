package com.danielagapov.spawn.shared.exceptions;

import static com.danielagapov.spawn.shared.util.UserField.EMAIL;

public class EmailAlreadyExistsException extends FieldAlreadyExistsException {
    public EmailAlreadyExistsException(String message) {
        super(message, EMAIL);
    }
}
