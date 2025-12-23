package com.danielagapov.spawn.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for email/password authentication and registration.
 * 
 * Note: This DTO is ONLY used for email/password flows where a password is required.
 * For OAuth authentication (Google, Apple), use OAuthRegistrationDTO instead, 
 * which does not have a password field.
 * 
 * Inherits validation from AbstractUserDTO for username, name, email, and bio.
 */
@NoArgsConstructor
@Getter
@Setter
public class AuthUserDTO extends AbstractUserDTO {
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    public AuthUserDTO(UUID id, String name, String email, String username, String bio, String password) {
        super(id, name, email, username, bio);
        this.password = password;
    }
}