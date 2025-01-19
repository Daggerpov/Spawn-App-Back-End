package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageDTO(
        UUID id,
        String content,
        Instant timestamp,
        UUID userSenderId,
        UUID eventId,
        List<UUID> likedByUserIds
) implements Serializable {}