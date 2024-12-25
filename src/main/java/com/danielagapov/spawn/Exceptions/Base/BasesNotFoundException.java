package com.danielagapov.spawn.Exceptions.Base;

import java.util.UUID;

public class BasesNotFoundException extends RuntimeException {
    public BasesNotFoundException(String type, UUID id) {
        super(type + " not found with UUID: " + id);
    }
    public BasesNotFoundException(String type) {
        super(type + " not found.");
    }
}