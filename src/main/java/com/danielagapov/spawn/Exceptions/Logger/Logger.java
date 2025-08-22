package com.danielagapov.spawn.Exceptions.Logger;

import com.danielagapov.spawn.Services.ErrorLog.IErrorLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class Logger implements ILogger {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Logger.class);
    
    @Autowired
    @Lazy // Avoid circular dependency
    private IErrorLogService errorLogService;

    public void info(String message) {
        logger.info(formatMessageWithCallerInfo(message));
    }

    public void warn(String message) {
        logger.warn(formatMessageWithCallerInfo(message));
    }

    public void error(String message) {
        String formattedMessage = formatMessageWithCallerInfo(message);
        logger.error(formattedMessage);
        
        // Create a synthetic exception for error logging
        RuntimeException syntheticException = new RuntimeException(message);
        
        // Extract user context from thread local or other sources if available
        String userContext = extractUserContext();
        
        // Log to error logging system asynchronously
        try {
            if (errorLogService != null) {
                errorLogService.logError(message, syntheticException, userContext);
            }
        } catch (Exception e) {
            // Don't let error logging break the main application
            logger.warn("Failed to log error to error logging system: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced error method that accepts a throwable
     */
    public void error(String message, Throwable throwable) {
        String formattedMessage = formatMessageWithCallerInfo(message);
        logger.error(formattedMessage, throwable);
        
        // Extract user context
        String userContext = extractUserContext();
        
        // Log to error logging system asynchronously
        try {
            if (errorLogService != null) {
                errorLogService.logError(message, throwable, userContext);
            }
        } catch (Exception e) {
            // Don't let error logging break the main application
            logger.warn("Failed to log error to error logging system: " + e.getMessage());
        }
    }
    
    /**
     * Formats a message with caller information (file name, line number, and method name)
     *
     * @param message The original message to format
     * @return A formatted message with caller information
     */
    private String formatMessageWithCallerInfo(String message) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Index 0 is getStackTrace, index 1 is this method, index 2 is the logging method (info/warn/error),
        // and index 3 is the actual caller we want
        StackTraceElement caller = stackTrace[3];

        String fileName = caller.getFileName();
        String methodName = caller.getMethodName();
        int lineNumber = caller.getLineNumber();

        // Format the message with caller information: [FileName:LineNumber] MethodName - Message
        return String.format("[%s:%d] %s - %s", fileName, lineNumber, methodName, message);
    }
    
    /**
     * Extract user context information if available
     * This could include current user ID, request details, etc.
     */
    private String extractUserContext() {
        try {
            // TODO: Implement user context extraction from SecurityContext, ThreadLocal, etc.
            // For now, return basic thread information
            Thread currentThread = Thread.currentThread();
            return String.format("Thread: %s", currentThread.getName());
        } catch (Exception e) {
            return "Context unavailable";
        }
    }
}
