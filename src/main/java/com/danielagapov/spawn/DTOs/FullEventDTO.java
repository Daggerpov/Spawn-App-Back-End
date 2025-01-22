package com.danielagapov.spawn.DTOs;


import com.danielagapov.spawn.Models.Location;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FullEventDTO(
        UUID id,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        LocationDTO location,
        String note,
        UserDTO creatorUser,
        List<UserDTO> participantUsers,
        List<UserDTO> invitedUsers,
        List<ChatMessageDTO> chatMessages
) implements Serializable, IEventDTO {}