package com.danielagapov.spawn.chat.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public abstract class AbstractChatMessageDTO implements Serializable {
    private UUID id;
    private String content;
    private Instant timestamp;
    private UUID activityId;
}
