package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.user.internal.domain.User;

import java.util.UUID;

/**
 * Utility methods for consistent logging across the application
 */
public final class LoggingUtils {

    /**
     * Format user information for logging, including ID, first name, last name, and username
     *
     * @param user The user entity
     * @return Formatted string with user details
     */
    public static String formatUserInfo(User user) {
        if (user == null) {
            return "null user";
        }
        return String.format("%s with name: %s and username: %s",
                user.getId(), user.getName(), user.getUsername());
    }

    /**
     * Format user ID for logging when only the ID is available
     * Include a note that the full user info is not available
     *
     * @param userId The user ID
     * @return Formatted string with user ID
     */
    public static String formatUserIdInfo(UUID userId) {
        if (userId == null) {
            return "null userId";
        }
        return userId.toString() + " (full user info not available)";
    }
} 