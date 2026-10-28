package com.danielagapov.spawn.chat.api.dto;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class FullActivityChatMessageDTO extends AbstractChatMessageDTO {
    private BaseUserDTO senderUser;
    private List<BaseUserDTO> likedByUsers;

    public FullActivityChatMessageDTO(UUID id, String content, Instant timestamp, BaseUserDTO senderUser, UUID activityId, List<BaseUserDTO> likedByUsers) {
        super(id, content, timestamp, activityId);
        this.senderUser = senderUser;
        this.likedByUsers = likedByUsers;
    }
}
