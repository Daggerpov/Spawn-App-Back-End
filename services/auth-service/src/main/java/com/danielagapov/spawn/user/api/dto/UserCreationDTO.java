package com.danielagapov.spawn.user.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for user creation (legacy OAuth flow).
 * Note: This inherits validation from AbstractUserDTO for username, name, email, and bio.
 * Password is NOT part of this DTO as it's used for OAuth flows.
 */
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