package com.danielagapov.spawn.Models.ChatMessage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MockChatMessage {
    @Id
    Long id;
    String timestamp;
    Long userSenderId;
    String content;
    Long eventId;

    public MockChatMessage() {
    }

    public MockChatMessage(Long id, String timestamp, Long userSenderId, String content, Long eventId) {
        this.id = id;
        this.timestamp = timestamp;
        this.userSenderId = userSenderId;
        this.content = content;
        this.eventId = eventId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Long getUserSenderId() {
        return userSenderId;
    }

    public void setUserSenderId(Long userSenderId) {
        this.userSenderId = userSenderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}