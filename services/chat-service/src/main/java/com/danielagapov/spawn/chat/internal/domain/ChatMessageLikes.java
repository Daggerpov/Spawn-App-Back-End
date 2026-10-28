package com.danielagapov.spawn.chat.internal.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

@Entity
@Table(name = "chat_message_like")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessageLikes implements Serializable {
    @EmbeddedId
    private ChatMessageLikesId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatMessageId")
    @JoinColumn(name = "chat_message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChatMessage chatMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserRef user;
}
