package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.OAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OAuthRegistrationDTO {
    private String email;
    private String idToken;  // Changed from externalIdToken to idToken
    private OAuthProvider provider;
    private String name;
    private String profilePictureUrl;
}
