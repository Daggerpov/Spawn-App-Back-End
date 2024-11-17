package com.danielagapov.spawn.Models.ChatMessage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity
public record ChatMessage (
        @Id
        Long id,
        String timestamp, // TODO: investigate data type later
        Long userSenderId,
        String content,
        Long eventId
) implements Serializable {
}