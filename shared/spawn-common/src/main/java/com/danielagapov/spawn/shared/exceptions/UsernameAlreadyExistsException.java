package com.danielagapov.spawn.shared.exceptions;

import static com.danielagapov.spawn.shared.util.UserField.USERNAME;

public class UsernameAlreadyExistsException extends FieldAlreadyExistsException {
    public UsernameAlreadyExistsException(String message) {
        super(message, USERNAME);
    }
}
