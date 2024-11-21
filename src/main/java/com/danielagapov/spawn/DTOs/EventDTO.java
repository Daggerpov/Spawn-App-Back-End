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
        LocationDTO location, // TODO: investigate data type later
        String note,
        UserDTO creator,
        List<UserDTO> participants,
        List<UserDTO> invited,
        List<ChatMessageDTO> chatMessages
) implements Serializable {}