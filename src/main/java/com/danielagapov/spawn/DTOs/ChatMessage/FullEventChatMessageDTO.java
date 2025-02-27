package com.danielagapov.spawn.DTOs.ChatMessage;

import com.danielagapov.spawn.DTOs.User.UserDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullEventChatMessageDTO extends AbstractChatMessageDTO {
    UserDTO senderUser;
    List<UserDTO> likedByUsers;

    public FullEventChatMessageDTO(UUID id, String content, Instant timestamp, UserDTO senderUser, UUID eventId, List<UserDTO> likedByUsers) {
        super(id, content, timestamp, eventId);
        this.senderUser = senderUser;
        this.likedByUsers = likedByUsers;
    }
}