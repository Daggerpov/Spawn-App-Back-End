package com.danielagapov.spawn.user.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String provider;  // Auth provider: "google", "apple", or "email"

    public BaseUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture) {
        super(id, name, email, username, bio);
        this.profilePicture = profilePicture;
        this.hasCompletedOnboarding = false; // Default value for backward compatibility
        this.provider = null;
    }

    public BaseUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture, Boolean hasCompletedOnboarding) {
        super(id, name, email, username, bio);
        this.profilePicture = profilePicture;
        this.hasCompletedOnboarding = hasCompletedOnboarding != null ? hasCompletedOnboarding : false;
        this.provider = null;
    }

    @JsonCreator
    public BaseUserDTO(
            @JsonProperty("id") UUID id, 
            @JsonProperty("name") String name, 
            @JsonProperty("email") String email, 
            @JsonProperty("username") String username, 
            @JsonProperty("bio") String bio, 
            @JsonProperty("profilePicture") String profilePicture, 
            @JsonProperty("hasCompletedOnboarding") Boolean hasCompletedOnboarding,
            @JsonProperty("provider") String provider) {
        super(id, name, email, username, bio);
        this.profilePicture = profilePicture;
        this.hasCompletedOnboarding = hasCompletedOnboarding != null ? hasCompletedOnboarding : false;
        this.provider = provider;
    }
}
