package com.danielagapov.spawn.Exceptions.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/*
 * A table consisting of many-to-one relationships of a User's Id and a
 * FriendTag's Id. This table is queried to find a user's association to a
 * FriendTag. Ex:
 *
 * If Seabert is a user with UUID 123, Seabert has Daniel as a friend with
 * UUID 321 and has a FriendTag called "spawn friends" with UUID 777.
 * This entity contains a user_id column containing 321 and a friend_tag_id
 * column containing 777 which is the value of the id column in the friendTag
 * table. Further, 123 is the value of the owner_id column in the FriendTag
 * table.
 */
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
    @GeneratedValue
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User friend;

    @ManyToOne
    @JoinColumn(name = "friend_tag_id", referencedColumnName = "id", nullable = false)
    private FriendTag friendTag;
}
