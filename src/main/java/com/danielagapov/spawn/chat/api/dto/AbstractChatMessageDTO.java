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

public abstract class AbstractChatMessageDTO implements Serializable{
    UUID id;
    String content;
    Instant timestamp;
    UUID activityId;
}
