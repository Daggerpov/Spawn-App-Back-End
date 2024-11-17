package com.danielagapov.spawn.Exceptions.Base;

public class BasesNotFoundException extends RuntimeException {
    public BasesNotFoundException() {
        super("Users not found.");
    }
}