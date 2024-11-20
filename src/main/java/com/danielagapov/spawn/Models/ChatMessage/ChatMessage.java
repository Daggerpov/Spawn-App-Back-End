package com.danielagapov.spawn.Models.ChatMessage;

import com.danielagapov.spawn.Models.Event.Event;
import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
// these two annotations are in place of writing out constructors (for readability)
@NoArgsConstructor
@AllArgsConstructor
// these two annotations are in place of writing out getters and setters manually (for readability):
@Getter
@Setter
public class ChatMessage implements Serializable {
        private @Id
        @GeneratedValue Long id;
        private Instant timestamp;
        private Long userSenderId;
        private String content;
        private Long eventId;
}