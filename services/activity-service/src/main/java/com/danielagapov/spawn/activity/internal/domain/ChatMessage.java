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
import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChatMessage implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(length = 1000)
    private String content;
    private Instant timestamp;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User userSender;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Activity activity;
}
