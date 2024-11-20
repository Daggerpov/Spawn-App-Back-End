package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageDTO(
        UUID id,
        Instant timestamp,
        UUID userSenderId,
        String content,
        UUID eventId
) implements Serializable {}