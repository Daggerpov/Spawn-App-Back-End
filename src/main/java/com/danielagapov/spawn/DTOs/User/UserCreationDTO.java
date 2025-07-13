package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class UserCreationDTO extends AbstractUserDTO {
    private byte[] profilePictureData; // raw image uploaded

    public UserCreationDTO(UUID id, String username, byte[] profilePictureData, String name, String bio, String email) {
        super(id, name, email, username, bio);
        this.profilePictureData = profilePictureData;
    }
}