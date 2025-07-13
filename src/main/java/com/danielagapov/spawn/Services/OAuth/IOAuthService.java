package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Models.User.User;

import java.util.Optional;

/**
 * Service interface for managing OAuth authentication operations.
 * Handles user authentication, account creation, and mapping between external OAuth providers and internal user accounts.
 */
public interface IOAuthService {

    /**
     * Given a new user dto, creates a new account which means saving the user info and their external id mapping
     *
     * @param user           new user to save
     * @param externalUserId user id from external provider
     * @param profilePicture byte arr of user's pfp
     * @param provider       oauth provider that the new user used to sign in with
     * @return BaseUserDTO of the newly created user
     * @throws org.springframework.dao.DataAccessException if database operations fail
     */
    BaseUserDTO makeUser(UserDTO user, String externalUserId, byte[] profilePicture, OAuthProvider provider);

    /**
     * Signs in a user using their OAuth ID token and email address.
     * Verifies the token, extracts the user ID, and checks if the user exists.
     * 
     * @param idToken the OAuth ID token to verify
     * @param email the user's email address
     * @param provider the OAuth provider used for authentication
     * @return Optional containing BaseUserDTO if user exists, empty otherwise
     * @throws com.danielagapov.spawn.Exceptions.IncorrectProviderException if user exists but with different provider
     * @throws SecurityException if token verification fails
     */
    Optional<BaseUserDTO> signInUser(String idToken, String email, OAuthProvider provider);

    /**
     * Given an external user id from an oauth provider, check whether it belongs it a user account.
     * First tries to find user by external id, then by email in case a user has signed in with a different provider
     *
     * @param externalUserId user id from external provider
     * @param email          user email
     * @return a BaseUserDTO if user exists, null otherwise
     * @throws com.danielagapov.spawn.Exceptions.IncorrectProviderException if user exists but with different provider
     */
    Optional<BaseUserDTO> getUserIfExistsbyExternalId(String externalUserId, String email);

    /**
     * Creates a user from either Google ID token or Apple external user ID
     * 
     * @param userCreationDTO DTO containing user creation details
     * @param idToken  ID token
     * @param provider OAuth provider (required for Apple)
     * @return Created or existing user BaseUserDTO
     * @throws IllegalArgumentException when required parameters are missing
     * @throws SecurityException when token validation fails
     */
    BaseUserDTO createUserFromOAuth(UserCreationDTO userCreationDTO, String idToken, OAuthProvider provider);

    String checkOAuthRegistration(String email, String idToken, OAuthProvider provider);

    void createAndSaveMapping(User user, String externalId, OAuthProvider provider);
}
