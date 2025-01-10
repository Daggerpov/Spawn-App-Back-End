package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.UserDTO;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface IOAuthService {

    UserDTO verifyUser(OAuth2User user);
}
