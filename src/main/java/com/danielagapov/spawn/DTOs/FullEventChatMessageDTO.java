package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FullEventChatMessageDTO(
        UUID id,
        String content,
        Instant timestamp,
        FullUserDTO senderUser,
        UUID eventId,
        List<FullUserDTO> likedByUsers
) implements Serializable, IChatMessageDTO{}