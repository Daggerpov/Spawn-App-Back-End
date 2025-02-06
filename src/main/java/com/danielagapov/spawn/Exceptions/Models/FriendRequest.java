package com.danielagapov.spawn.Exceptions.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(
        name = "friend_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sender_id", "receiver_id"})
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FriendRequest implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
}
