package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class UserCreationDTO extends AbstractUserDTO implements Serializable {
    private byte[] profilePictureData; // raw image uploaded

    public UserCreationDTO(UUID id, String username, byte[] profilePictureData, String firstName, String lastName, String bio, String email) {
        super(id, firstName, lastName, email, username, bio);
        this.profilePictureData = profilePictureData;
    }
}