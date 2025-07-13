package com.danielagapov.spawn.Exceptions;

/**
 * Exception thrown when an OAuth provider service is unavailable
 */
public class OAuthProviderUnavailableException extends RuntimeException {
    public OAuthProviderUnavailableException(String message) {
        super(message);
    }
    
    public OAuthProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
} 