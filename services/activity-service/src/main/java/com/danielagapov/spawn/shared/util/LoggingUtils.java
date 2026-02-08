package com.danielagapov.spawn.shared.util;

import java.util.UUID;

public final class LoggingUtils {
    public static String formatUserIdInfo(UUID userId) {
        if (userId == null) return "null userId";
        return userId.toString() + " (full user info not available)";
    }
}
