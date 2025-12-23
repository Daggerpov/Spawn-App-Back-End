package com.danielagapov.spawn.user.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class AuthUserDTO extends AbstractUserDTO {
    private String password;

    public AuthUserDTO(UUID id, String name, String email, String username, String bio, String password) {
        super(id, name, email, username, bio);
        this.password = password;
    }
}