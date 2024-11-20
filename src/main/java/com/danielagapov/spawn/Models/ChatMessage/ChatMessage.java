package com.danielagapov.spawn.Models.ChatMessage;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import java.io.Serializable;

@Entity
class ChatMessage implements Serializable {
        private @Id
        @GeneratedValue Long id;
        private String timestamp; // TODO: investigate data type later
        private Long userSenderId;
        private String content;
        private Long eventId;

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

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }
}