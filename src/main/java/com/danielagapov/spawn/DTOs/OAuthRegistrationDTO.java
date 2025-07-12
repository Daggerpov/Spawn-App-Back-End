package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.OAuthProvider;
import lombok.Getter;

@Getter
public class OAuthRegistrationDTO {
    private final String email;
    private final String externalIdToken;
    private final OAuthProvider provider;
    private final String name;
    private final String profilePictureUrl;

    public OAuthRegistrationDTO(String email, String externalIdToken, OAuthProvider provider, String name, String profilePictureUrl) {
        this.email = email;
        this.externalIdToken = externalIdToken;
        this.provider = provider;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
    }
}
