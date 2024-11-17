package com.danielagapov.spawn.Exceptions.User;

import java.util.List;

public class UsersNotFoundException extends RuntimeException {
    public UsersNotFoundException(List<Long> ids) {
        super("Users not found with requested IDs: " + ids);
    }
}