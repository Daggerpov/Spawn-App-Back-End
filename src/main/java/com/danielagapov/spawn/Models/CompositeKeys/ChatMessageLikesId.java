package com.danielagapov.spawn.Models.CompositeKeys;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessageLikesId implements Serializable {
    @Column(name = "chat_message_id", nullable = false)
    private Long chatMessageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}