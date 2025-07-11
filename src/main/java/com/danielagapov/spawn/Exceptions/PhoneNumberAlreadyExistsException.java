package com.danielagapov.spawn.Exceptions;

import static com.danielagapov.spawn.Enums.UserField.PHONE_NUMBER;

public class PhoneNumberAlreadyExistsException extends FieldAlreadyExistsException {
    public PhoneNumberAlreadyExistsException(String message) {
        super(message, PHONE_NUMBER);
    }
}
