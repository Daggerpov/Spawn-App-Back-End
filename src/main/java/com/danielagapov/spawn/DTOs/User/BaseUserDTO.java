package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BaseUserDTO extends AbstractUserDTO {
    private String profilePicture;

    public BaseUserDTO(UUID id, String firstName, String lastName, String email, String username, String bio, String profilePicture) {
        super(id, firstName, lastName, email, username, bio);
        this.profilePicture = profilePicture;
    }
}
