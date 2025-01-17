package com.danielagapov.spawn.Helpers;

import org.springframework.core.MethodParameter;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

// this class follows the Singleton design pattern
// access it through UUIDValidator.shared.validateUUID(), for example
public class UUIDValidator {
    private static UUIDValidator instance;
    private final MethodParameter dummyMethodParameter;

    // Private constructor to prevent instantiation
    private UUIDValidator() {
        try {
            // Create a dummy MethodParameter for the exception
            this.dummyMethodParameter = new MethodParameter(
                    UUIDValidator.class.getDeclaredMethod("validateUUID", String.class), 0);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to initialize UUIDValidator", e);
        }
    }

    // Static method to provide the singleton instance
    public static UUIDValidator getInstance() {
        if (instance == null) {
            synchronized (UUIDValidator.class) {
                if (instance == null) {
                    instance = new UUIDValidator();
                }
            }
        }
        return instance;
    }

    public UUID validateUUID(String id) {
        if (id == null || id.isBlank()) {
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
