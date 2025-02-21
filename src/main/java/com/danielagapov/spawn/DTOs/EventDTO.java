package com.danielagapov.spawn.DTOs;


import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class EventDTO extends AbstractEventDTO implements Serializable, IEventDTO{
    UUID locationId;
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
        super.id = id;
        super.title = title;
        super.startTime = startTime;
        super.endTime = endTime;
        this.locationId = locationId;
        super.note = note;
        super.creatorUserId = creatorUserId;
        this.participantUserIds = participantUserIds;
        this.invitedUserIds = invitedUserIds;
        this.chatMessageIds = chatMessageIds;
    }

}