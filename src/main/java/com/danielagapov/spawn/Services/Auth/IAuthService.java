package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.EmailVerificationResponseDTO;
import com.danielagapov.spawn.DTOs.OAuthRegistrationDTO;
import com.danielagapov.spawn.DTOs.User.*;
import com.danielagapov.spawn.Models.User.User;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

public interface IAuthService {
    /**
     * Registers the user by creating
     */
    UserDTO registerUser(AuthUserDTO authUserDTO);

    BaseUserDTO loginUser(String usernameOrEmail, String password);

    boolean verifyEmail(String token);
    
    /**
     * Changes a user's password after verifying the current password
     * @param username the username of the user
     * @param currentPassword the current password
     * @param newPassword the new password
     * @return true if password change was successful
     */
    boolean changePassword(String username, String currentPassword, String newPassword);

    AuthResponseDTO getUserByToken(String token);

    /**
     * Sends an email verification code to the specified email address for new user registration
     * @param email the email address to send the verification code to
     * @return response containing seconds until next attempt and message
     */
    EmailVerificationResponseDTO sendEmailVerificationCodeForRegistration(String email);

    /**
     * Checks the email verification code and creates a new user upon successful verification
     * @param email the email address that received the verification code
     * @param code the verification code to check
     * @return the created user DTO if verification is successful
     */
    BaseUserDTO checkEmailVerificationCode(String email, String code);

    /**
     * Registers a new user via OAuth (Google or Apple)
     *
     * @return the created user DTO with status for onboarding navigation
     */
    AuthResponseDTO registerUserViaOAuth(OAuthRegistrationDTO registrationDTO);

    /**
     * Handles OAuth registration gracefully by converting exceptions to appropriate user states
     * @param registrationDTO the OAuth registration data
     * @param exception the exception that occurred during registration
     * @return a graceful AuthResponseDTO that guides the user to the appropriate next step
     */
    AuthResponseDTO handleOAuthRegistrationGracefully(OAuthRegistrationDTO registrationDTO, Exception exception);

    /**
     * Helper method to call access/refresh token-generating methods and place them in the appropriate
     * HTTP headers
     */
    HttpHeaders makeHeadersForTokens(String username);

    /**
     * Helper method to generate tokens for users, using email as fallback when username is null
     * This is specifically needed for OAuth users during onboarding who don't have usernames yet
     */
    HttpHeaders makeHeadersForTokens(User user);

    /**
     * Updates user details (username, phone number, password) for an existing user
     * @param dto the update details DTO
     * @return the updated user DTO
     */
    BaseUserDTO updateUserDetails(UpdateUserDetailsDTO dto);

    /**
     * Mark contact import step as completed
     * @param userId the ID of the user who completed contact import
     * @return the updated user DTO
     */
    BaseUserDTO completeContactImport(UUID userId);

    /**
     * Update user status to ACTIVE (for Terms of Service acceptance)
     * @param userId the ID of the user to update
     * @return the updated user DTO
     */
    BaseUserDTO acceptTermsOfService(UUID userId);

    /**
     * Cancels onboarding for a user with status < ACTIVE by deleting their account
     * This is called when the user chooses to not resume an existing onboarding session on the client
     * @param userId id of user to delete
     */
    void cancelOnboarding(UUID userId);
}
