package com.danielagapov.spawn.Helpers;

import com.danielagapov.spawn.Helpers.Logger.ILogger;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

// This class follows the Singleton design pattern
// Access it through UUIDValidator.getInstance(logger).validateUUID(), for example
public class UUIDValidator {
    private static UUIDValidator instance;
    private final MethodParameter dummyMethodParameter;
    private final ILogger logger;

    // Private constructor to prevent instantiation
    private UUIDValidator(ILogger logger) {
        this.logger = logger;
        try {
            // Create a dummy MethodParameter for the exception
            this.dummyMethodParameter = new MethodParameter(
                    UUIDValidator.class.getDeclaredMethod("validateUUID", String.class), 0);
        } catch (NoSuchMethodException e) {
            this.logger.log("Error initializing UUIDValidator: " + e.getMessage());
            throw new RuntimeException("Failed to initialize UUIDValidator", e);
        }
    }

    // Static method to provide the singleton instance with logger injection
    public static UUIDValidator getInstance(ILogger logger) {
        if (instance == null) {
            synchronized (UUIDValidator.class) {
                if (instance == null) {
                    instance = new UUIDValidator(logger);
                }
            }
        }
        return instance;
    }

    // Overloaded method to retrieve the existing instance without passing a logger
    public static UUIDValidator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UUIDValidator is not initialized. Call getInstance(ILogger) first.");
        }
        return instance;
    }

    public UUID validateUUID(String id) {
        if (id == null || id.isBlank()) {
            logger.log("Validation failed: ID is null or blank.");
            throw new MethodArgumentTypeMismatchException(
                    id,
                    UUID.class,
                    "id",
                    dummyMethodParameter,
                    new IllegalArgumentException("ID cannot be null or blank")
            );
        }

        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            logger.log("Validation failed: " + e.getMessage());
            throw new MethodArgumentTypeMismatchException(
                    id,
                    UUID.class,
                    "id",
                    dummyMethodParameter,
                    e
            );
        }
    }
}
