package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(
        name = "user_friend_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_tag_id"}) // Ensure unique user-tag pairs
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserFriendTag implements Serializable {
    @Id
    @GeneratedValue
    private UUID id; //TODO do we need this?

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User Friend1;

    @ManyToOne
    @JoinColumn(name = "friend_tag_id", referencedColumnName = "id", nullable = false)
    private FriendTag friendTag;
}