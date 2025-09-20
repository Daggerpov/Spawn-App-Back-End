package com.danielagapov.spawn.DTOs.Activity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for external activity invites - contains only essential information
 * needed for the activity invite page without requiring authentication
 */
@NoArgsConstructor
@Getter
@Setter
public class ActivityInviteDTO extends AbstractActivityDTO {
    UUID locationId;
    UUID activityTypeId;
    UUID creatorUserId;
    List<UUID> participantUserIds;
    List<UUID> invitedUserIds;
    
    public ActivityInviteDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    UUID locationId,
    UUID activityTypeId,
    String note,
    String icon,
    Integer participantLimit,
    UUID creatorUserId,
    List<UUID> participantUserIds,
    List<UUID> invitedUserIds,
    Instant createdAt,
    boolean isExpired,
    String clientTimezone) {
        super(id, title, startTime, endTime, note, icon, participantLimit, createdAt, isExpired, clientTimezone);
        this.locationId = locationId;
        this.activityTypeId = activityTypeId;
        this.creatorUserId = creatorUserId;
        this.participantUserIds = participantUserIds;
        this.invitedUserIds = invitedUserIds;
    }
} 