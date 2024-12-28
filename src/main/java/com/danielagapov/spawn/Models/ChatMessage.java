package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessage implements Serializable {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID id;

        @Column(length = 1000)
        private String content; // Can be null or empty

        private Instant timestamp;

        @ManyToOne
        @JoinColumn(name = "user_id", nullable = false)
        private User userSender;

        @ManyToOne
        @JoinColumn(name = "event_id", nullable = false)
        private Event event;

        private List<User> likedBy;
}
