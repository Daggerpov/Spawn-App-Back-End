package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.AuthUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;

public interface IAuthService {
    UserDTO registerUser(AuthUserDTO authUserDTO);

    FullUserDTO loginUser(AuthUserDTO authUserDTO);
}
