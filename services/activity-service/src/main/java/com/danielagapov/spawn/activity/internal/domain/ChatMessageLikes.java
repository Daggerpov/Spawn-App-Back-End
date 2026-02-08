package com.danielagapov.spawn.activity.internal.domain;

import com.danielagapov.spawn.user.internal.domain.User;
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
