package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.OAuthProvider;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
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
}
