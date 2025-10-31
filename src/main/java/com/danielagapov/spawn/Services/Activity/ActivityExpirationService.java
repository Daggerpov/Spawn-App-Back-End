package com.danielagapov.spawn.Services.Activity;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Centralized service for handling activity expiration logic.
 * This serves as the single source of truth for all expiration-related decisions
 * across the application (feed display, share links, etc.).
 */
@Service
public final class ActivityExpirationService {

    /**
     * Determines if an activity is expired based on its end time and creation time.
     * This is the authoritative method for activity expiration across the entire application.
     * 
     * Expiration Rules (per original specification):
     * 1. Activities with endTime: expired if endTime < current time (UTC)
     * 2. Activities without endTime: expire at the end of the day they were created (UTC)
     * 3. Activities with no creation time: never expire
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @param createdAt The activity creation time (can be null)
     * @return true if the activity is expired, false otherwise
     */
    public boolean isActivityExpired(OffsetDateTime startTime, OffsetDateTime endTime, Instant createdAt) {
        return isActivityExpired(startTime, endTime, createdAt, null);
    }

    /**
     * Determines if an activity is expired based on its end time, creation time, and client timezone.
     * This method supports timezone-aware expiration for activities without explicit end times.
     * 
     * Expiration Rules:
     * 1. Activities with endTime: expired if endTime < current time (UTC)
     * 2. Activities without endTime but with clientTimezone: expire at midnight (12 AM) following day in client timezone
     * 3. Activities without endTime and without clientTimezone: expire at the end of the day they were created (UTC)
     * 4. Activities with no creation time: never expire
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @param createdAt The activity creation time (can be null)
     * @param clientTimezone The timezone of the client that created the activity (can be null)
     * @return true if the activity is expired, false otherwise
     */
    public boolean isActivityExpired(OffsetDateTime startTime, OffsetDateTime endTime, Instant createdAt, String clientTimezone) {
        // Use UTC for consistent timezone comparison across server and client timezones
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        // Activities with explicit end time
        if (endTime != null) {
            // Convert endTime to UTC for proper comparison
            OffsetDateTime endTimeUtc = endTime.withOffsetSameInstant(ZoneOffset.UTC);
            return endTimeUtc.isBefore(now);
        }
        
        // Activities without end time expire at the end of the day they were created
        if (createdAt != null) {
            if (clientTimezone != null && !clientTimezone.trim().isEmpty()) {
                try {
                    // Use client timezone for expiration calculation
                    ZoneId clientZone = ZoneId.of(clientTimezone);
                    
                    // Get the date the activity was created in the client's timezone
                    LocalDate createdDate = createdAt.atZone(clientZone).toLocalDate();
                    
                    // Calculate midnight (12 AM) of the following day in client timezone
                    OffsetDateTime endOfCreationDay = createdDate.plusDays(1)
                        .atStartOfDay(clientZone)
                        .toOffsetDateTime()
                        .withOffsetSameInstant(ZoneOffset.UTC);
                    
                    return now.isAfter(endOfCreationDay);
                } catch (Exception e) {
                    // If timezone parsing fails, fall back to UTC behavior
                    // Log the error but continue with UTC fallback to ensure system stability
                }
            }
            
            // Fallback to UTC behavior (original logic)
            // Get the date the activity was created (in UTC)
            LocalDate createdDate = createdAt.atOffset(ZoneOffset.UTC).toLocalDate();
            // Calculate end of that day (23:59:59.999 UTC)
            OffsetDateTime endOfCreationDay = createdDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
            return now.isAfter(endOfCreationDay);
        }
        
        // Activities with no creation time never expire (shouldn't happen in practice)
        return false;
    }

    /**
     * Calculates when an activity will expire.
     * Used for share link expiration and other time-based operations.
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @param createdAt The activity creation time (can be null)
     * @return The expiration time in UTC, or null if the activity never expires
     */
    public OffsetDateTime calculateActivityExpiration(OffsetDateTime startTime, OffsetDateTime endTime, Instant createdAt) {
        return calculateActivityExpiration(startTime, endTime, createdAt, null);
    }

    /**
     * Calculates when an activity will expire, considering client timezone.
     * Used for share link expiration and other time-based operations.
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @param createdAt The activity creation time (can be null)
     * @param clientTimezone The timezone of the client that created the activity (can be null)
     * @return The expiration time in UTC, or null if the activity never expires
     */
    public OffsetDateTime calculateActivityExpiration(OffsetDateTime startTime, OffsetDateTime endTime, Instant createdAt, String clientTimezone) {
        // Activities with explicit end time expire at their end time
        if (endTime != null) {
            return endTime.withOffsetSameInstant(ZoneOffset.UTC);
        }
        
        // Activities without end time expire at the end of the day they were created
        if (createdAt != null) {
            if (clientTimezone != null && !clientTimezone.trim().isEmpty()) {
                try {
                    // Use client timezone for expiration calculation
                    ZoneId clientZone = ZoneId.of(clientTimezone);
                    
                    // Get the date the activity was created in the client's timezone
                    LocalDate createdDate = createdAt.atZone(clientZone).toLocalDate();
                    
                    // Return midnight (12 AM) of the following day in client timezone, converted to UTC
                    return createdDate.plusDays(1)
                        .atStartOfDay(clientZone)
                        .toOffsetDateTime()
                        .withOffsetSameInstant(ZoneOffset.UTC);
                } catch (Exception e) {
                    // If timezone parsing fails, fall back to UTC behavior
                    // Log the error but continue with UTC fallback to ensure system stability
                }
            }
            
            // Fallback to UTC behavior (original logic)
            // Get the date the activity was created (in UTC)
            LocalDate createdDate = createdAt.atOffset(ZoneOffset.UTC).toLocalDate();
            // Return end of that day (start of next day)
            return createdDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        }
        
        // Activities with no creation time never expire
        return null;
    }

    /**
     * Calculates when a share link for an activity should expire.
     * Share links expire 1 day after the activity itself expires.
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @param createdAt The activity creation time (can be null)
     * @return The share link expiration time, or null if it should never expire
     */
    public OffsetDateTime calculateShareLinkExpiration(OffsetDateTime startTime, OffsetDateTime endTime, Instant createdAt) {
        return calculateShareLinkExpiration(startTime, endTime, createdAt, null);
    }

    /**
     * Calculates when a share link for an activity should expire, considering client timezone.
     * Share links expire 1 day after the activity itself expires.
     * 
     * @param startTime The activity start time (can be null)
     * @param endTime The activity end time (can be null)
     * @param createdAt The activity creation time (can be null)
     * @param clientTimezone The timezone of the client that created the activity (can be null)
     * @return The share link expiration time, or null if it should never expire
     */
    public OffsetDateTime calculateShareLinkExpiration(OffsetDateTime startTime, OffsetDateTime endTime, Instant createdAt, String clientTimezone) {
        OffsetDateTime activityExpiration = calculateActivityExpiration(startTime, endTime, createdAt, clientTimezone);
        
        if (activityExpiration != null) {
            // Share link expires 1 day after activity expires
            return activityExpiration.plusDays(1);
        }
        
        // If activity never expires, share link also never expires
        return null;
    }
}
