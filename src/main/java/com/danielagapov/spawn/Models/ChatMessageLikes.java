package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(
        name = "chat_message_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chat_message_id", "user_id"})
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessageLikes implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
