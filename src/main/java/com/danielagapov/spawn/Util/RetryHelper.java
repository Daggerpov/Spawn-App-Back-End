package com.danielagapov.spawn.Util;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * Utility class for implementing retry logic with exponential backoff
 */
public class RetryHelper {
    
    /**
     * Executes a callable with retry logic using exponential backoff
     * 
     * @param callable The operation to retry
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelay Initial delay before first retry
     * @param retryOnException Predicate to determine if exception should trigger retry
     * @param <T> Return type of the callable
     * @return Result of the successful operation
     * @throws Exception The last exception if all retries fail
     */
    public static <T> T executeWithRetry(
            Callable<T> callable, 
            int maxRetries, 
            Duration initialDelay,
            Predicate<Exception> retryOnException) throws Exception {
        
        Exception lastException = null;
        Duration currentDelay = initialDelay;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                
                if (attempt == maxRetries || !retryOnException.test(e)) {
                    throw e;
                }
                
                // Wait before retrying
                try {
                    Thread.sleep(currentDelay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                
                // Exponential backoff
                currentDelay = currentDelay.multipliedBy(2);
            }
        }
        
        throw lastException;
    }
    
    /**
     * Convenience method for OAuth-related retries
     */
    public static <T> T executeOAuthWithRetry(Callable<T> callable) throws Exception {
        return executeWithRetry(
            callable, 
            3, 
            Duration.ofMillis(500),
            exception -> isRetryableException(exception)
        );
    }
    
    /**
     * Determines if an exception is retryable for OAuth operations
     */
    private static boolean isRetryableException(Exception e) {
        // Network timeouts, connection errors, etc.
        return e instanceof java.net.ConnectException
            || e instanceof java.net.SocketTimeoutException
            || e instanceof java.io.IOException
            || (e.getCause() != null && isRetryableException((Exception) e.getCause()));
    }
} 