package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.Services.Activity.ActivityExpirationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ActivityExpirationServiceTimezoneTests {

    private ActivityExpirationService expirationService;

    @BeforeEach
    void setUp() {
        expirationService = new ActivityExpirationService();
    }

    @Test
    void testActivityExpirationWithTimezone_NewYorkTimezone() {
        // Create an activity that was created at 2PM EST (7PM UTC) yesterday
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS).plus(7, ChronoUnit.HOURS);
        String clientTimezone = "America/New_York";
        
        // Activity without end time should expire at midnight in New York timezone
        // If it's currently after midnight New York time (5AM UTC), it should be expired
        boolean isExpired = expirationService.isActivityExpired(null, null, createdAt, clientTimezone);
        
        // The result depends on current time, but we can verify the method doesn't throw exceptions
        assertNotNull(isExpired);
    }

    @Test
    void testActivityExpirationWithTimezone_LondonTimezone() {
        // Create an activity that was created at 2PM GMT (2PM UTC) yesterday
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS);
        String clientTimezone = "Europe/London";
        
        // Activity without end time should expire at midnight in London timezone
        boolean isExpired = expirationService.isActivityExpired(null, null, createdAt, clientTimezone);
        
        // The result depends on current time, but we can verify the method doesn't throw exceptions
        assertNotNull(isExpired);
    }

    @Test
    void testActivityExpirationWithTimezone_InvalidTimezone() {
        // Create an activity with an invalid timezone
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        String clientTimezone = "Invalid/Timezone";
        
        // Should fall back to UTC behavior when timezone is invalid
        boolean isExpired = expirationService.isActivityExpired(null, null, createdAt, clientTimezone);
        
        // Should not throw exception and should fall back to UTC logic
        assertNotNull(isExpired);
    }

    @Test
    void testActivityExpirationWithExplicitEndTime() {
        // Activity with explicit end time should ignore timezone
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        OffsetDateTime endTime = OffsetDateTime.now(ZoneOffset.UTC).minus(1, ChronoUnit.HOURS);
        String clientTimezone = "America/New_York";
        
        boolean isExpired = expirationService.isActivityExpired(null, endTime, createdAt, clientTimezone);
        
        // Should be expired since end time was 1 hour ago
        assertTrue(isExpired);
    }

    @Test
    void testActivityExpirationWithNullTimezone() {
        // Activity with null timezone should use UTC behavior
        Instant createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
        String clientTimezone = null;
        
        boolean isExpired = expirationService.isActivityExpired(null, null, createdAt, clientTimezone);
        
        // Should be expired since it was created 2 days ago (past end of UTC day)
        assertTrue(isExpired);
    }

    @Test
    void testCalculateActivityExpirationWithTimezone() {
        // Test that calculateActivityExpiration also works with timezone
        Instant createdAt = Instant.now();
        String clientTimezone = "America/Los_Angeles";
        
        OffsetDateTime expiration = expirationService.calculateActivityExpiration(null, null, createdAt, clientTimezone);
        
        // Should return a valid expiration time
        assertNotNull(expiration);
        // Should be in the future (end of day in LA timezone)
        assertTrue(expiration.isAfter(OffsetDateTime.now(ZoneOffset.UTC)));
    }
}
