package com.danielagapov.spawn.activity.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class ActivityDTO extends AbstractActivityDTO {
    LocationDTO location;
    UUID activityTypeId;
    UUID creatorUserId;
    List<UUID> participantUserIds;
    List<UUID> invitedUserIds;
    List<UUID> chatMessageIds;
    String clientTimezone; // Timezone of the client creating the activity (e.g., "America/New_York")
    
    public ActivityDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    LocationDTO location,
    UUID activityTypeId,
    String note,
    String icon,
    Integer participantLimit,
    UUID creatorUserId,
    List<UUID> participantUserIds,
    List<UUID> invitedUserIds,
    List<UUID> chatMessageIds,
    Instant createdAt,
    boolean isExpired,
    String clientTimezone) {
        super(id, title, startTime, endTime, note, icon, participantLimit, createdAt, isExpired, clientTimezone);
        this.location = location;
        this.activityTypeId = activityTypeId;
        this.creatorUserId = creatorUserId;
        this.participantUserIds = participantUserIds;
        this.invitedUserIds = invitedUserIds;
        this.chatMessageIds = chatMessageIds;
        this.clientTimezone = clientTimezone;
    }
}