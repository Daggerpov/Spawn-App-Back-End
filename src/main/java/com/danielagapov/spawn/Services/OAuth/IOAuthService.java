package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface IOAuthService {

    AbstractUserDTO verifyUser(OAuth2User user);
    UserDTO makeUser(UserDTO user, String externId);
    FullUserDTO getUserIfExistsbyExternalId(String externId);
}
