package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EventCreationDTO(
        UUID id,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        LocationDTO location,
        String note,
        UUID creatorUserId,
        List<UUID> invitedFriendTagIds,
        List<UUID> invitedFriendUserIds
) implements Serializable {}

