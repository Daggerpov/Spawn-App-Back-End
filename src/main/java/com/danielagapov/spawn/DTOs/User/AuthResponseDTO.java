package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.Enums.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponseDTO extends AbstractUserDTO {
    private String profilePicture;
    private UserStatus status;

    public AuthResponseDTO(UUID id, String name, String email, String username, String bio, String profilePicture, UserStatus status) {
        super(id, name, email, username, bio);
        this.profilePicture = profilePicture;
        this.status = status;
    }
} 