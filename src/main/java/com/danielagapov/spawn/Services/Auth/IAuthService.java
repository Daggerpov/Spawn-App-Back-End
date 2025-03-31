package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;

public interface IAuthService {
    /**
     * Registers the user by creating
     */
    UserDTO registerUser(AuthUserDTO authUserDTO);

    BaseUserDTO loginUser(AuthUserDTO authUserDTO);

    boolean verifyEmail(String token);
}
