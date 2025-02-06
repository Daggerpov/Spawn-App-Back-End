package com.danielagapov.spawn.Exceptions.Models.CompositeKeys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessageLikesId implements Serializable {
    @Column(name = "chat_message_id", nullable = false)
    private UUID chatMessageId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}