package com.danielagapov.spawn.shared.exceptions;

import com.danielagapov.spawn.shared.util.UserField;

public class AccountAlreadyExistsException extends FieldAlreadyExistsException {
    public AccountAlreadyExistsException(String message) {
        super(message, UserField.EXTERNAL_ID);
    }
}
