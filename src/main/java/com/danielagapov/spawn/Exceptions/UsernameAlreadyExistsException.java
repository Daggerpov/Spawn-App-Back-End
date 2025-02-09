package com.danielagapov.spawn.Exceptions;

import static com.danielagapov.spawn.Enums.Field.USERNAME;

public class UsernameAlreadyExistsException extends FieldAlreadyExistsException {
    public UsernameAlreadyExistsException(String message) {
        super(message, USERNAME);
    }
}
