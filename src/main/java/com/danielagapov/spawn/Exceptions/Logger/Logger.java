package com.danielagapov.spawn.Exceptions.Logger;

import org.springframework.stereotype.Service;

@Service
public class Logger implements ILogger {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Logger.class);

    public void info(String message) {
        logger.info(formatMessageWithCallerInfo(message));
    }

    public void warn(String message) {
        logger.warn(formatMessageWithCallerInfo(message));
    }

    public void error(String message) {
        logger.error(formatMessageWithCallerInfo(message));
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
}
