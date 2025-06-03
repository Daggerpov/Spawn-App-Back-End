package com.danielagapov.spawn.DTOs.Activity;

import com.danielagapov.spawn.Enums.ActivityCategory;
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
    UUID creatorUserId;
    List<UUID> invitedFriendUserIds;
    
    public ActivityCreationDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    LocationDTO location,
    String note,
    String icon,
    ActivityCategory category,
    UUID creatorUserId,
    List<UUID> invitedFriendUserIds,
    Instant createdAt) {
        super(id, title, startTime, endTime, note, icon, category, createdAt);
        this.location = location;
        this.creatorUserId = creatorUserId;
        this.invitedFriendUserIds = invitedFriendUserIds;
    }
}

