package com.danielagapov.spawn.Exceptions.User;

import java.util.List;

public class UsersNotFoundException extends RuntimeException {
    public UsersNotFoundException() {
        super("Users not found.");
    }
}