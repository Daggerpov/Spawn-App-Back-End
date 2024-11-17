package com.danielagapov.spawn.Exceptions.User;

public class UserSaveException extends RuntimeException {
    public UserSaveException(String message) {
        super(message);
    }
}