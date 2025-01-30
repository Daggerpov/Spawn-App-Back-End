package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface IOAuthService {

    AbstractUserDTO verifyUser(OAuth2User user);
    UserDTO makeUser(UserDTO user, String externalUserId, byte[] profilePicture, OAuthProvider provider);
    FullUserDTO getUserIfExistsbyExternalId(String externalUserId, String email, OAuthProvider provider);
}
