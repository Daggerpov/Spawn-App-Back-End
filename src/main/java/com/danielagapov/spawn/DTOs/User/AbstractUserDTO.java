package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
// Abstract class since interface describes behaviours
public abstract class AbstractUserDTO implements Serializable {
    UUID id;
    String firstName;
    String lastName;
    String email;
    String username;
    String bio;

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof AbstractUserDTO other))
            return false;
        return other.getId().equals(this.id) && other.getFirstName().equals(this.firstName) &&
                other.getLastName().equals(this.lastName) && other.getEmail().equals(this.email) &&
                other.getUsername().equals(this.username) && other.getBio().equals(this.bio);
    }
}
