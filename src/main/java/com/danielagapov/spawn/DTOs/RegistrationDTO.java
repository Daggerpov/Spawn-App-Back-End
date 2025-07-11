package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.OAuthProvider;
import lombok.Getter;

@Getter
public class RegistrationDTO {
    private final String email;
    private final String externalIdToken;
    private final OAuthProvider provider;

    public RegistrationDTO(String email, String externalIdToken, OAuthProvider provider) {
        this.email = email;
        this.externalIdToken = externalIdToken;
        this.provider = provider;
    }
}
