package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Getter
public class AuthUserDTO extends AbstractUserDTO implements Serializable {
    private final String password;

    public AuthUserDTO(UUID id, String firstName, String lastName, String email, String username, String bio, String password) {
        super(id, firstName, lastName, email, username, bio);
        this.password = password;
    }
}