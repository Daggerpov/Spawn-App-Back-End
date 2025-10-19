package com.danielagapov.spawn.Exceptions.Logger;

public interface ILogger {
    void info(String message);

    void warn(String message);

    void error(String message);
    
    void error(String message, Throwable throwable);
}
