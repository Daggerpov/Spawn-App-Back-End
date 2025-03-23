package com.danielagapov.spawn.DTOs.ChatMessage;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullEventChatMessageDTO extends AbstractChatMessageDTO {
    BaseUserDTO senderUser;
    List<BaseUserDTO> likedByUsers;

    public FullEventChatMessageDTO(UUID id, String content, Instant timestamp, BaseUserDTO senderUser, UUID eventId, List<BaseUserDTO> likedByUsers) {
        super(id, content, timestamp, eventId);
        this.senderUser = senderUser;
        this.likedByUsers = likedByUsers;
    }
}