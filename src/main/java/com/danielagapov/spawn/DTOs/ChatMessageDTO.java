package com.danielagapov.spawn.DTOs;

import java.io.Serializable;

public record ChatMessageDTO(
        Long id,
        String timestamp,
        Long userSenderId,
        String content,
        Long eventId
) implements Serializable {}