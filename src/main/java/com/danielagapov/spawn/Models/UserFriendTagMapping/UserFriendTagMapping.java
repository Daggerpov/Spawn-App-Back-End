package com.danielagapov.spawn.Models.UserFriendTagMapping;

import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.FriendTag.FriendTag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(
        name = "user_friend_tag_mapping",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_1", "user_2", "friend_tag_id"})
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserFriendTagMapping implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_1", nullable = false)
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user_2", nullable = false)
    private User user2;

    @ManyToOne
    @JoinColumn(name = "friend_tag_id", nullable = false)
    private FriendTag friendTag;
}