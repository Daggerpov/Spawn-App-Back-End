package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.activity.api.dto.CalendarActivityDTO;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.repositories.IActivityRepository;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;
import com.danielagapov.spawn.activity.internal.services.CalendarService;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.CacheEvictionHelper;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.user.internal.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Calendar dates follow the client's timezone when it sends one (Daggerpov/Spawn-App-Back-End#476). */
@ExtendWith(MockitoExtension.class)
class CalendarServiceTimezoneTests {

    @Mock private ILogger logger;
    @Mock private IActivityRepository activityRepository;
    @Mock private IActivityUserRepository activityUserRepository;
    @Mock private CacheEvictionHelper cacheEvictionHelper;

    private CalendarService calendarService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(logger, activityRepository, activityUserRepository, cacheEvictionHelper);
    }

    /** The user's only activity starts at the given UTC time. */
    private void givenActivityAt(OffsetDateTime startUtc) {
        User creator = new User();
        creator.setId(userId);
        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setTitle("Late Night Basketball");
        activity.setIcon("🏀");
        activity.setStartTime(startUtc);
        activity.setCreator(creator);
        when(activityRepository.findByCreatorId(userId)).thenReturn(List.of(activity));
        when(activityUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating)).thenReturn(List.of());
    }

    // 10:37 PM on Nov 17 in Vancouver is 06:37 on Nov 18 in UTC
    private static final OffsetDateTime EVENING_NOV_17_VANCOUVER = OffsetDateTime.of(2026, 11, 18, 6, 37, 0, 0, ZoneOffset.UTC);

    @Test
    @DisplayName("Places an evening activity on the local date when a timezone is sent")
    void usesClientTimezone() {
        givenActivityAt(EVENING_NOV_17_VANCOUVER);
        List<CalendarActivityDTO> result = calendarService.getCalendarActivitiesWithFilters(userId, 11, 2026, "America/Vancouver");
        assertEquals(1, result.size());
        assertEquals("2026-11-17", result.get(0).getDate());
    }

    @Test
    @DisplayName("Keeps UTC dates when no timezone is sent")
    void keepsUtcWithoutTimezone() {
        givenActivityAt(EVENING_NOV_17_VANCOUVER);
        List<CalendarActivityDTO> result = calendarService.getCalendarActivitiesWithFilters(userId, 11, 2026);
        assertEquals("2026-11-18", result.get(0).getDate());
    }

    @Test
    @DisplayName("Falls back to UTC for an unknown timezone")
    void fallsBackToUtcForUnknownTimezone() {
        givenActivityAt(EVENING_NOV_17_VANCOUVER);
        List<CalendarActivityDTO> result = calendarService.getCalendarActivitiesWithFilters(userId, 11, 2026, "Not/A_Zone");
        assertEquals("2026-11-18", result.get(0).getDate());
    }

    @Test
    @DisplayName("Uses the timezone for the month filter too")
    void filtersMonthInClientTimezone() {
        // 9 PM on Nov 30 in Vancouver is 05:00 on Dec 1 in UTC, so it belongs to November locally
        givenActivityAt(OffsetDateTime.of(2026, 12, 1, 5, 0, 0, 0, ZoneOffset.UTC));
        assertEquals(1, calendarService.getCalendarActivitiesWithFilters(userId, 11, 2026, "America/Vancouver").size());
        assertEquals(0, calendarService.getCalendarActivitiesWithFilters(userId, 11, 2026).size());
    }
}
