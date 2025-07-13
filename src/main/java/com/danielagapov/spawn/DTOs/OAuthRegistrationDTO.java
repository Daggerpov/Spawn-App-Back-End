package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.OAuthProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class OAuthRegistrationDTO {
    @JsonProperty("email")
    private final String email;
    @JsonProperty("externalIdToken")
    private final String externalIdToken;
    @JsonProperty("provider")
    private final OAuthProvider provider;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("profilePictureUrl")
    private final String profilePictureUrl;

    public OAuthRegistrationDTO(String email, String externalIdToken, OAuthProvider provider, String name, String profilePictureUrl) {
        this.email = email;
        this.externalIdToken = externalIdToken;
        this.provider = provider;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
    }
}
