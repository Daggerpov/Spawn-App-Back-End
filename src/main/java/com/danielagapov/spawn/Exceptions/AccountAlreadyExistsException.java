package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Enums.UserField;

public class AccountAlreadyExistsException extends FieldAlreadyExistsException {
    public AccountAlreadyExistsException(String message) {
        super(message, UserField.EXTERNAL_ID);
    }
}
