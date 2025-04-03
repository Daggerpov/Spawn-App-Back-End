package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/*
 * Represents a unique Spawn User.
 * The allFriends UUID field represents each user's unique "everyone" tag which is a special kind of FriendTag
 * holding all the user's friends. Each user must have such a tag.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User implements Serializable {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true) // Ensures the username is unique and not null
    private String username;
    private String profilePictureUrlString;

    private String firstName;
    private String lastName;
    private String bio;
    private String email;
    private String password;
    private boolean verified;
    private Date dateCreated;
    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private NotificationPreferences notificationPreferences;


    public User(UUID id, String username, String profilePictureUrlString, String firstName, String lastName, String bio, String email) {
        this.id = id;
        this.username = username;
        this.profilePictureUrlString = profilePictureUrlString;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bio = bio;
        this.email = email;
    }
}
