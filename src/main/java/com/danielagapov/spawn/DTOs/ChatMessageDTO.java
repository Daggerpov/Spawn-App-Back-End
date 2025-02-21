package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class ChatMessageDTO implements Serializable, IChatMessageDTO{
    UUID id;
    String content;
    Instant timestamp;
    UUID senderUserId;
    UUID eventId;
    List<UUID> likedByUserIds;
}