package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter

public class AbstractChatMessageDTO implements Serializable{
    UUID id;
    String content;
    Instant timestamp;
    UUID eventId;
}
