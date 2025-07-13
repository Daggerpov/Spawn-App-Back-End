package com.danielagapov.spawn.Exceptions;

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