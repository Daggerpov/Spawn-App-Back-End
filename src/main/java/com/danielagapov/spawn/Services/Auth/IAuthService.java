package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.AuthUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.Map;

public interface IAuthService {
    Map<String, Object> registerUser(AuthUserDTO authUserDTO);
    Map<String, Object> loginUser(AuthUserDTO authUserDTO); // TODO: return FullUserDTO
}
