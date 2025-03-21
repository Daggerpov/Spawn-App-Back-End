package com.danielagapov.spawn.DTOs.Event;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EventCreationDTO extends AbstractEventDTO {
    LocationDTO location;
    UUID creatorUserId;
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
        super(id, title, startTime, endTime, note);
        this.location = location;
        this.creatorUserId = creatorUserId;
        this.invitedFriendTagIds = invitedFriendTagIds;
        this.invitedFriendUserIds = invitedFriendUserIds;
    }
}

