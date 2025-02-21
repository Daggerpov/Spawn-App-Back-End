package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullEventChatMessageDTO extends AbstractChatMessageDTO {
    FullUserDTO senderUser;
    List<FullUserDTO> likedByUsers;

    public FullEventChatMessageDTO(UUID id, String content, Instant timestamp, FullUserDTO senderUser, UUID eventId, List<FullUserDTO> likedByUsers) {
        super(id, content, timestamp, eventId);
        this.senderUser = senderUser;
        this.likedByUsers = likedByUsers;
    }
}