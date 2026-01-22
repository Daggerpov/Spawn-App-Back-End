package com.danielagapov.spawn.auth.api.dto;

import com.danielagapov.spawn.shared.util.OAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthSignInRequestDTO {
    private String idToken;
    private OAuthProvider provider;
    private String email;
}
