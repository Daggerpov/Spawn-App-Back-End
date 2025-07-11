package com.danielagapov.spawn.DTOs.Activity;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ActivityCreationDTO extends AbstractActivityDTO {
    LocationDTO location;
    UUID activityTypeId;
    UUID creatorUserId;
    List<UUID> invitedFriendUserIds;
    
    public ActivityCreationDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    LocationDTO location,
    UUID activityTypeId,
    String note,
    String icon,
    UUID creatorUserId,
    List<UUID> invitedFriendUserIds,
    Instant createdAt) {
        super(id, title, startTime, endTime, note, icon, createdAt);
        this.location = location;
        this.activityTypeId = activityTypeId;
        this.creatorUserId = creatorUserId;
        this.invitedFriendUserIds = invitedFriendUserIds;
    }
}

