package com.danielagapov.spawn.auth.api.dto;

import com.danielagapov.spawn.shared.util.OAuthProvider;
import com.danielagapov.spawn.shared.validation.ValidName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OAuthRegistrationDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "ID token is required")
    private String idToken;  // Changed from externalIdToken to idToken
    
    @NotNull(message = "OAuth provider is required")
    private OAuthProvider provider;
    
    @ValidName(optional = true)
    private String name;
    
    private String profilePictureUrl;
}
