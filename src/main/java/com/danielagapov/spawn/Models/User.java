package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;
import java.util.List;

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
}