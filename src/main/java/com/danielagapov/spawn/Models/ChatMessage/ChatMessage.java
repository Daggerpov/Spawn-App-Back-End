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
@Table(name = "ChatMessage")
public class ChatMessage implements Serializable {
        @Id
        @GeneratedValue
        @Column(name = "Id", nullable = false, updatable = false)
        private UUID id;

        @Column(name = "Timestamp", nullable = false)
        private Instant timestamp;

        @Column(name = "Content", nullable = false)
        private String content;

        @ManyToOne
        @JoinColumn(name = "User", referencedColumnName = "id")
        private User user;

        @ManyToOne
        @JoinColumn(name = "EventId", referencedColumnName = "id")
        private Event event;
}