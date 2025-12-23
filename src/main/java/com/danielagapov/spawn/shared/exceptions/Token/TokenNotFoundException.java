package com.danielagapov.spawn.shared.exceptions.Token;

public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
