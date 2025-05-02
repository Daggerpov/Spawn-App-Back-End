package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.CompositeKeys.ChatMessageLikesId;
import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

/**
 * A chat message like is left by someone onto a
 * chat message (see documentation in `ChatMessage.java`).
 * These should behave like toggles, to where one user
 * can either like a message or not, so it's a 1-to-1 relationship.
 * Also, it seems most apps like Instagram, Slack, discord allow you
 * to react to/like your own message, so we'll leave that open
 * for our own users as well.
 */
@Entity
@Table(name = "chat_message_like")
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChatMessage chatMessage;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
}