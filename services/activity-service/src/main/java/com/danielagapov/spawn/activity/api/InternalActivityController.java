package com.danielagapov.spawn.activity.api;

import com.danielagapov.spawn.activity.api.dto.*;
import com.danielagapov.spawn.activity.internal.services.IActivityTypeService;
import com.danielagapov.spawn.activity.internal.services.ICalendarService;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/activities/internal")
public class InternalActivityController {

    private final IActivityService activityService;
    private final ICalendarService calendarService;
    private final IActivityTypeService activityTypeService;

    public InternalActivityController(IActivityService activityService, ICalendarService calendarService,
                                      IActivityTypeService activityTypeService) {
        this.activityService = activityService;
        this.calendarService = calendarService;
        this.activityTypeService = activityTypeService;
    }

    @GetMapping("created-by/{userId}")
    public ResponseEntity<List<UUID>> getActivityIdsCreatedByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(activityService.getActivityIdsCreatedByUser(userId));
    }

    @GetMapping("by-user")
    public ResponseEntity<List<UUID>> getActivityIdsByUserAndStatus(
            @RequestParam UUID userId,
            @RequestParam ParticipationStatus status) {
        return ResponseEntity.ok(activityService.getActivityIdsByUserIdAndStatus(userId, status));
    }

    @GetMapping("{activityId}/participant-ids")
    public ResponseEntity<List<UUID>> getParticipantUserIds(
            @PathVariable UUID activityId,
            @RequestParam ParticipationStatus status) {
        return ResponseEntity.ok(activityService.getParticipantUserIdsByActivityIdAndStatus(activityId, status));
    }

    @GetMapping("{activityId}/creator-id")
    public ResponseEntity<UUID> getCreatorId(@PathVariable UUID activityId) {
        return ResponseEntity.ok(activityService.getActivityCreatorId(activityId));
    }

    @GetMapping("{activityId}/title")
    public ResponseEntity<String> getActivityTitle(@PathVariable UUID activityId) {
        return ResponseEntity.ok(activityService.getActivityTitle(activityId));
    }

    @GetMapping("past")
    public ResponseEntity<List<UUID>> getPastActivityIdsForUser(
            @RequestParam UUID userId,
            @RequestParam ParticipationStatus status,
            @RequestParam(defaultValue = "10") int limit) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return ResponseEntity.ok(activityService.getPastActivityIdsForUser(userId, status, now, Limit.of(limit)));
    }

    @PostMapping("other-users")
    public ResponseEntity<List<UserIdActivityTimeDTO>> getOtherUsersByActivities(
            @RequestBody List<UUID> activityIds,
            @RequestParam UUID excludeUserId,
            @RequestParam ParticipationStatus status) {
        return ResponseEntity.ok(activityService.getOtherUserIdsByActivityIds(activityIds, excludeUserId, status));
    }

    @GetMapping("shared-count")
    public ResponseEntity<Integer> getSharedActivitiesCount(
            @RequestParam UUID userId1,
            @RequestParam UUID userId2,
            @RequestParam ParticipationStatus status) {
        return ResponseEntity.ok(activityService.getSharedActivitiesCount(userId1, userId2, status));
    }

    @GetMapping("calendar")
    public ResponseEntity<List<CalendarActivityDTO>> getCalendarActivities(
            @RequestParam UUID userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(calendarService.getCalendarActivitiesWithFilters(userId, month, year));
    }

    @GetMapping("timestamps/created")
    public ResponseEntity<Instant> getLatestCreatedTimestamp(@RequestParam UUID userId) {
        return ResponseEntity.ok(activityService.getLatestCreatedActivityTimestamp(userId));
    }

    @GetMapping("timestamps/invited")
    public ResponseEntity<Instant> getLatestInvitedTimestamp(@RequestParam UUID userId) {
        return ResponseEntity.ok(activityService.getLatestInvitedActivityTimestamp(userId));
    }

    @GetMapping("timestamps/updated")
    public ResponseEntity<Instant> getLatestUpdatedTimestamp(@RequestParam UUID userId) {
        return ResponseEntity.ok(activityService.getLatestUpdatedActivityTimestamp(userId));
    }

    @GetMapping("activity-types/{userId}")
    public ResponseEntity<List<ActivityTypeDTO>> getActivityTypesByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(activityTypeService.getActivityTypesByUserId(userId));
    }

    @PostMapping("calendar/clear-all")
    public ResponseEntity<Void> clearAllCalendarCaches() {
        calendarService.clearAllCalendarCaches();
        return ResponseEntity.ok().build();
    }

    @PostMapping("activity-types/initialize-for-user/{userId}")
    public ResponseEntity<Void> initializeActivityTypesForUser(@PathVariable UUID userId) {
        activityTypeService.initializeDefaultActivityTypesForUserId(userId);
        return ResponseEntity.ok().build();
    }
}
