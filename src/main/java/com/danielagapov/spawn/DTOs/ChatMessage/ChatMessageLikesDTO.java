package com.danielagapov.spawn.DTOs.ChatMessage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class ChatMessageLikesDTO implements Serializable{
    UUID chatMessageId;
    UUID userId;
}
