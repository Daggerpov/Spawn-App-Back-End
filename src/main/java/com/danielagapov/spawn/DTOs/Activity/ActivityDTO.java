package com.danielagapov.spawn.DTOs.Activity;

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
    UUID locationId;
    UUID activityTypeId;
    UUID creatorUserId;
    List<UUID> participantUserIds;
    List<UUID> invitedUserIds;
    List<UUID> chatMessageIds;
    
    public ActivityDTO(UUID id,
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
    List<UUID> chatMessageIds,
    Instant createdAt) {
        super(id, title, startTime, endTime, note, icon, participantLimit, createdAt);
        this.locationId = locationId;
        this.activityTypeId = activityTypeId;
        this.creatorUserId = creatorUserId;
        this.participantUserIds = participantUserIds;
        this.invitedUserIds = invitedUserIds;
        this.chatMessageIds = chatMessageIds;
    }
}