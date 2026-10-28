package com.danielagapov.spawn.shared.exceptions;

import static com.danielagapov.spawn.shared.util.UserField.PHONE_NUMBER;

public class PhoneNumberAlreadyExistsException extends FieldAlreadyExistsException {
    public PhoneNumberAlreadyExistsException(String message) {
        super(message, PHONE_NUMBER);
    }
}
