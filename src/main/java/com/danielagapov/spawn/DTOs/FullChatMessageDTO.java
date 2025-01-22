package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FullChatMessageDTO(
        UUID id,
        String content,
        Instant timestamp,
        UserDTO senderUser,
        EventDTO event,
        List<UserDTO> likedByUsers
) implements Serializable, IChatMessageDTO{}