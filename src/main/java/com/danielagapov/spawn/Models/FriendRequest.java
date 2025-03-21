package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.UUID;

/**
 * A friend request is a one-way entity from a
 * sender user to a receiver user, to which the
 * receiver user can accept on their mobile client.
 * Once they've accepted the friend request, it's
 * deleted from this entity, and they're made into
 * friends by being added as each other's friends (
 * through the "Everyone" tag).
 */
@Entity
@Table(
        name = "friend_request",
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User receiver;
}
