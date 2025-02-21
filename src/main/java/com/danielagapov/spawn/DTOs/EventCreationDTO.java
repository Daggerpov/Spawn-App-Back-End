package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EventCreationDTO extends AbstractEventDTO implements Serializable {
    LocationDTO location;
    List<UUID> invitedFriendTagIds;
    List<UUID> invitedFriendUserIds;
    public EventCreationDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    LocationDTO location,
    String note,
    UUID creatorUserId,
    List<UUID> invitedFriendTagIds,
    List<UUID> invitedFriendUserIds) {
        super.id = id;
        super.title = title;
        super.startTime = startTime;
        super.endTime = endTime;
        this.location = location;
        super.note = note;
        super.creatorUserId = creatorUserId;
        this.invitedFriendTagIds = invitedFriendTagIds;
        this.invitedFriendUserIds = invitedFriendUserIds;
    }
}

