package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BaseUserDTO extends AbstractUserDTO {
    private String profilePicture;
    private Boolean hasCompletedOnboarding;

    public BaseUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture) {
        super(id, name, email, username, bio);
        this.profilePicture = profilePicture;
    }

    public BaseUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture, Boolean hasCompletedOnboarding) {
        super(id, name, email, username, bio);
        this.profilePicture = profilePicture;
        this.hasCompletedOnboarding = hasCompletedOnboarding;
    }
}
