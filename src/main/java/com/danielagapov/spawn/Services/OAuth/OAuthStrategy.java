package com.danielagapov.spawn.Services.OAuth;


import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;

import java.util.Optional;

/**
 * Strategy interface for different OAuth providers
 */
public interface OAuthStrategy {

    Optional<BaseUserDTO> getUserIfExistsByToken(String idToken, String email);

    OAuthProvider getOAuthProvider();

    BaseUserDTO createUserWithToken(UserCreationDTO userCreationDTO, String idToken);
}
