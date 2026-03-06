package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.activity.api.dto.*;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "activity-service-client",
        url = "${services.activity-service.url:http://localhost:8082}"
)
public interface ActivityServiceClient {

    // ==================== Internal Query Endpoints ====================

    @GetMapping("/api/v1/activities/internal/created-by/{userId}")
    List<UUID> getActivityIdsCreatedByUser(@PathVariable("userId") UUID userId);

    @GetMapping("/api/v1/activities/internal/by-user")
    List<UUID> getActivityIdsByUserAndStatus(
            @RequestParam("userId") UUID userId,
            @RequestParam("status") ParticipationStatus status);

    @GetMapping("/api/v1/activities/internal/{activityId}/participant-ids")
    List<UUID> getParticipantUserIds(
            @PathVariable("activityId") UUID activityId,
            @RequestParam("status") ParticipationStatus status);

    @GetMapping("/api/v1/activities/internal/{activityId}/creator-id")
    UUID getCreatorId(@PathVariable("activityId") UUID activityId);

    @GetMapping("/api/v1/activities/internal/{activityId}/title")
    String getActivityTitle(@PathVariable("activityId") UUID activityId);

    @GetMapping("/api/v1/activities/internal/past")
    List<UUID> getPastActivityIdsForUser(
            @RequestParam("userId") UUID userId,
            @RequestParam("status") ParticipationStatus status,
            @RequestParam("limit") int limit);

    @PostMapping("/api/v1/activities/internal/other-users")
    List<UserIdActivityTimeDTO> getOtherUsersByActivities(
            @RequestBody List<UUID> activityIds,
            @RequestParam("excludeUserId") UUID excludeUserId,
            @RequestParam("status") ParticipationStatus status);

    @GetMapping("/api/v1/activities/internal/shared-count")
    Integer getSharedActivitiesCount(
            @RequestParam("userId1") UUID userId1,
            @RequestParam("userId2") UUID userId2,
            @RequestParam("status") ParticipationStatus status);

    @GetMapping("/api/v1/activities/internal/calendar")
    List<CalendarActivityDTO> getCalendarActivities(
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year);

    @GetMapping("/api/v1/activities/internal/timestamps/created")
    Instant getLatestCreatedActivityTimestamp(@RequestParam("userId") UUID userId);

    @GetMapping("/api/v1/activities/internal/timestamps/invited")
    Instant getLatestInvitedActivityTimestamp(@RequestParam("userId") UUID userId);

    @GetMapping("/api/v1/activities/internal/timestamps/updated")
    Instant getLatestUpdatedActivityTimestamp(@RequestParam("userId") UUID userId);

    @GetMapping("/api/v1/activities/internal/activity-types/{userId}")
    List<ActivityTypeDTO> getActivityTypesByUserId(@PathVariable("userId") UUID userId);

    @PostMapping("/api/v1/activities/internal/calendar/clear-all")
    Void clearAllCalendarCaches();

    @PostMapping("/api/v1/activities/internal/activity-types/initialize-for-user/{userId}")
    Void initializeActivityTypesForUser(@PathVariable("userId") UUID userId);

    // ==================== Public API Endpoints (used by CacheService, ShareLink) ====================

    @GetMapping("/api/v1/activities/feed-activities/{requestingUserId}")
    List<FullFeedActivityDTO> getFeedActivities(@PathVariable("requestingUserId") UUID requestingUserId);

    @GetMapping("/api/v1/activities/profile/{profileUserId}")
    List<ProfileActivityDTO> getProfileActivities(
            @PathVariable("profileUserId") UUID profileUserId,
            @RequestParam("requestingUserId") UUID requestingUserId);

    @GetMapping("/api/v1/activities/{id}")
    FullFeedActivityDTO getFullActivityById(
            @PathVariable("id") UUID id,
            @RequestParam(value = "requestingUserId", required = false) UUID requestingUserId);
}
