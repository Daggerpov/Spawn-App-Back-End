package com.danielagapov.spawn.Models.User;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
// these two annotations are in place of writing out constructors manually (for readability)
@NoArgsConstructor
@AllArgsConstructor
// these two annotations are in place of writing out getters and setters manually (for readability):
@Getter
@Setter
public class User  implements Serializable {
        private @Id
        @GeneratedValue UUID id;
        private String username;
        private String firstName;
        private String lastName;
        private String bio;
        private String profilePicture; // TODO: reconsider data type later
}
