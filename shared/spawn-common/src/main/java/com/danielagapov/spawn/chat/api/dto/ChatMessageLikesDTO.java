package com.danielagapov.spawn.chat.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessageLikesDTO implements Serializable {
    private UUID chatMessageId;
    private UUID userId;
}
