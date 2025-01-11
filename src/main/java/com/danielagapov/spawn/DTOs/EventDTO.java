package com.danielagapov.spawn.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EventDTO(
        UUID id,
        String title,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") // Ensures correct format for JSON Serialization
        OffsetDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") // Ensures correct format for JSON Serialization
        OffsetDateTime endTime,
        LocationDTO location,
        String note,
        UserDTO creator,
        List<UserDTO> participants,
        List<UserDTO> invited,
        List<ChatMessageDTO> chatMessages
) implements Serializable {}
