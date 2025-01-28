package com.danielagapov.spawn.DTOs;


import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EventDTO(
        UUID id,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        UUID locationId,
        String note,
        UUID creatorUserId,
        List<UUID> participantUserIds,
        List<UUID> invitedUserIds,
        List<UUID> chatMessageIds
) implements Serializable, IEventDTO{}