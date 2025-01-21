package com.danielagapov.spawn.DTOs;


import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FullEventDTO(
        UUID id,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        UUID locationId,
        String note,
        UUID creatorUserId,
        List<UserDTO> participantUsers,
        List<UserDTO> invitedUsers,
        List<ChatMessageDTO> chatMessages
) implements Serializable {}