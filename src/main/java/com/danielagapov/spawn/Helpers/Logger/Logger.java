package com.danielagapov.spawn.Helpers.Logger;

import org.springframework.stereotype.Service;

@Service
public class Logger implements ILogger{
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
