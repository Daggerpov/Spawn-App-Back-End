package com.danielagapov.spawn.shared.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Utility for activity expiration logic (share links, etc.).
 * Extracted from ActivityExpirationService for use by monolith after activity extraction.
 */
public final class ActivityExpirationUtil {

    private ActivityExpirationUtil() {}

    /**
     * Calculates when a share link for an activity should expire.
     * Share links expire 1 day after the activity itself expires.
     */
    public static OffsetDateTime calculateShareLinkExpiration(
            OffsetDateTime startTime, OffsetDateTime endTime, Instant createdAt) {
        OffsetDateTime activityExpiration = calculateActivityExpiration(startTime, endTime, createdAt, null);
        return activityExpiration != null ? activityExpiration.plusDays(1) : null;
    }

    private static OffsetDateTime calculateActivityExpiration(
            OffsetDateTime startTime, OffsetDateTime endTime, Instant createdAt, String clientTimezone) {
        if (endTime != null) {
            return endTime.withOffsetSameInstant(ZoneOffset.UTC);
        }
        if (createdAt != null) {
            if (clientTimezone != null && !clientTimezone.trim().isEmpty()) {
                try {
                    ZoneId clientZone = ZoneId.of(clientTimezone);
                    LocalDate createdDate = createdAt.atZone(clientZone).toLocalDate();
                    return createdDate.plusDays(1)
                            .atStartOfDay(clientZone)
                            .toOffsetDateTime()
                            .withOffsetSameInstant(ZoneOffset.UTC);
                } catch (Exception ignored) {
                    // Fall through to UTC behavior
                }
            }
            LocalDate createdDate = createdAt.atOffset(ZoneOffset.UTC).toLocalDate();
            return createdDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        }
        return null;
    }
}
