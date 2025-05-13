package com.danielagapov.spawn.DTOs.Event;

import com.danielagapov.spawn.Enums.EventCategory;
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
    String colorHexCode;
    
    public EventCreationDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    LocationDTO location,
    String note,
    String icon,
    EventCategory category,
    UUID creatorUserId,
    List<UUID> invitedFriendTagIds,
    List<UUID> invitedFriendUserIds,
    String colorHexCode) {
        super(id, title, startTime, endTime, note, icon, category, colorHexCode);
        this.location = location;
        this.creatorUserId = creatorUserId;
        this.invitedFriendTagIds = invitedFriendTagIds;
        this.invitedFriendUserIds = invitedFriendUserIds;
        this.colorHexCode = colorHexCode;
    }
}

