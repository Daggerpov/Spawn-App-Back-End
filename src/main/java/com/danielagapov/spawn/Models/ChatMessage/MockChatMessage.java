package com.danielagapov.spawn.Models.ChatMessage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class MockChatMessage {
    @Id
    UUID id;
    String timestamp;
    UUID userSenderId;
    String content;
    UUID eventId;

    public MockChatMessage() {
    }

    public MockChatMessage(UUID id, String timestamp, UUID userSenderId, String content, UUID eventId) {
        this.id = id;
        this.timestamp = timestamp;
        this.userSenderId = userSenderId;
        this.content = content;
        this.eventId = eventId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getUserSenderId() {
        return userSenderId;
    }

    public void setUserSenderId(UUID userSenderId) {
        this.userSenderId = userSenderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
}