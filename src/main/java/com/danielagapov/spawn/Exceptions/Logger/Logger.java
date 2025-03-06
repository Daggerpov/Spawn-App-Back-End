package com.danielagapov.spawn.Exceptions.Logger;

import org.springframework.stereotype.Service;

@Service
public class Logger implements ILogger {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Logger.class);

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void log(String message) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Get the caller's stack trace element (index 2 since index 0 is getStackTrace and index 1 is this log method)
        StackTraceElement caller = stackTrace[2];

        String fileName = caller.getFileName();
        String methodName = caller.getMethodName();
        int lineNumber = caller.getLineNumber();

        // Prints the details in a readable format ([FileName:LineNumber] MethodName - Message)
        System.out.printf("[%s:%d] %s - %s%n", fileName, lineNumber, methodName, message);
    }
}
