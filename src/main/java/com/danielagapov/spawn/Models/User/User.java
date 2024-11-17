package com.danielagapov.spawn.Models.User;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity
public record User(
        @Id
        Long id,
        String username,
        String firstName,
        String lastName,
        String bio
) implements Serializable {
}
