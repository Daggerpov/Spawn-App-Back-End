package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.CompositeKeys.ChatMessageLikesId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "chat_message_likes")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessageLikes implements Serializable {
    @EmbeddedId
    private ChatMessageLikesId id;

    @ManyToOne
    @MapsId("chatMessageId")
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}