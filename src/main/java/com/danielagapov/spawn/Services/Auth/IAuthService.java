package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;

public interface IAuthService {
    /**
     * Registers the user by creating
     */
    BaseUserDTO registerUser(AuthUserDTO authUserDTO);

    BaseUserDTO loginUser(AuthUserDTO authUserDTO);

    boolean verifyEmail(String token);
    
    /**
     * Changes a user's password after verifying the current password
     * @param username the username of the user
     * @param currentPassword the current password
     * @param newPassword the new password
     * @return true if password change was successful
     */
    boolean changePassword(String username, String currentPassword, String newPassword);

    BaseUserDTO getUserByToken(String token);
}
