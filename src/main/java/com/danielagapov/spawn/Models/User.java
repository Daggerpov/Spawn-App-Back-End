package com.danielagapov.spawn.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/*
 * Represents a unique Spawn User.
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
        private String profilePicture; // TODO: reconsider data type later
        private String firstName;
        private String lastName;
        private String bio;
        private String email;
        private UUID allFriends;
}
