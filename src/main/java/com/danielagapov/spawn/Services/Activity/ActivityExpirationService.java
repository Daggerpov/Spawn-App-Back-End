package com.danielagapov.spawn.Services.Activity;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Centralized service for handling activity expiration logic.
 * This serves as the single source of truth for all expiration-related decisions
 * across the application (feed display, share links, etc.).
 */
@Service
public class ActivityExpirationService {

    /**
     * Determines if an activity is expired based on its end time and start time.
     * This is the authoritative method for activity expiration across the entire application.
     * 
     * Expiration Rules:
     * 1. Activities with endTime: expired if endTime < current time (UTC)
     * 2. Indefinite activities (no endTime): expired if startTime + 24 hours < current time (UTC)
     * 3. Activities with no startTime or endTime: never expire
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @return true if the activity is expired, false otherwise
     */
    public boolean isActivityExpired(OffsetDateTime startTime, OffsetDateTime endTime) {
        // Use UTC for consistent timezone comparison across server and client timezones
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        // Activities with explicit end time
        if (endTime != null) {
            // Convert endTime to UTC for proper comparison
            OffsetDateTime endTimeUtc = endTime.withOffsetSameInstant(ZoneOffset.UTC);
            return endTimeUtc.isBefore(now);
        }
        
        // Indefinite activities (no end time) - expire 24 hours after start time
        if (startTime != null) {
            // Convert startTime to UTC and add 24 hours
            OffsetDateTime startTimeUtc = startTime.withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime indefiniteExpirationTime = startTimeUtc.plusHours(24);
            return indefiniteExpirationTime.isBefore(now);
        }
        
        // Activities with no start or end time never expire
        return false;
    }

    /**
     * Calculates when an activity will expire.
     * Used for share link expiration and other time-based operations.
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @return The expiration time in UTC, or null if the activity never expires
     */
    public OffsetDateTime calculateActivityExpiration(OffsetDateTime startTime, OffsetDateTime endTime) {
        // Activities with explicit end time expire at their end time
        if (endTime != null) {
            return endTime.withOffsetSameInstant(ZoneOffset.UTC);
        }
        
        // Indefinite activities expire 24 hours after start time
        if (startTime != null) {
            OffsetDateTime startTimeUtc = startTime.withOffsetSameInstant(ZoneOffset.UTC);
            return startTimeUtc.plusHours(24);
        }
        
        // Activities with no start or end time never expire
        return null;
    }

    /**
     * Calculates when a share link for an activity should expire.
     * Share links expire 1 day after the activity itself expires.
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @return The share link expiration time, or null if it should never expire
     */
    public OffsetDateTime calculateShareLinkExpiration(OffsetDateTime startTime, OffsetDateTime endTime) {
        OffsetDateTime activityExpiration = calculateActivityExpiration(startTime, endTime);
        
        if (activityExpiration != null) {
            // Share link expires 1 day after activity expires
            return activityExpiration.plusDays(1);
        }
        
        // If activity never expires, share link also never expires
        return null;
    }
}
