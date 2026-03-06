package com.danielagapov.spawn.shared.exceptions;

/**
 * Exception thrown when an OAuth token has expired
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
    
    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
} 