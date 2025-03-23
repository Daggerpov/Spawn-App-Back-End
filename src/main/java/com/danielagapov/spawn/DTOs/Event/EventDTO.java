package com.danielagapov.spawn.DTOs.Event;


import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EventDTO extends AbstractEventDTO {
    UUID locationId;
    UUID creatorUserId;
    List<UUID> participantUserIds;
    List<UUID> invitedUserIds;
    List<UUID> chatMessageIds;
    public EventDTO(    UUID id,
                                String title,
                                OffsetDateTime startTime,
                                OffsetDateTime endTime,
                                UUID locationId,
                                String note,
                                UUID creatorUserId,
                                List<UUID> participantUserIds,
                                List<UUID> invitedUserIds,
                                List<UUID> chatMessageIds) {
        super(id, title, startTime, endTime, note);
        this.creatorUserId = creatorUserId;
        this.locationId = locationId;
        this.participantUserIds = participantUserIds;
        this.invitedUserIds = invitedUserIds;
        this.chatMessageIds = chatMessageIds;
    }

}