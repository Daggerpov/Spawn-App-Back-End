package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import org.springframework.http.HttpHeaders;

public interface IAuthService {
    /**
     * Registers the user by creating
     */
    UserDTO registerUser(AuthUserDTO authUserDTO);

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

    /**
     * Sends an email verification code to the specified email address for new user registration
     * @param email the email address to send the verification code to
     */
    void sendEmailVerificationCodeForRegistration(String email);

    /**
     * Checks the email verification code and creates a new user upon successful verification
     * @param email the email address that received the verification code
     * @param code the verification code to check
     * @return the created user DTO if verification is successful
     */
    BaseUserDTO checkEmailVerificationCode(String email, String code);

    /**
     * Registers a new user via OAuth (Google or Apple)
     * @param email the user's email
     * @param externalIdToken the OAuth token
     * @param provider the OAuth provider
     * @return the created user DTO
     */
    BaseUserDTO registerUserViaOAuth(String email, String externalIdToken, OAuthProvider provider);

    /**
     * Helper method to call access/refresh token-generating methods and place them in the appropriate
     * HTTP headers
     */
    HttpHeaders makeHeadersForTokens(String username);
}
