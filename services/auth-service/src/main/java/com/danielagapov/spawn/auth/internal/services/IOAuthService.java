package com.danielagapov.spawn.auth.internal.services;

import com.danielagapov.spawn.user.api.dto.AuthResponseDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserCreationDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.shared.util.OAuthProvider;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.Optional;
import java.util.UUID;

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
     * @return Optional containing AuthResponseDTO if user exists, empty otherwise
     * @throws com.danielagapov.spawn.Exceptions.IncorrectProviderException if user exists but with different provider
     * @throws SecurityException if token verification fails
     */
    Optional<AuthResponseDTO> signInUser(String idToken, String email, OAuthProvider provider);

    /**
     * Given an external user id from an oauth provider, check whether it belongs it a user account.
     * First tries to find user by external id, then by email in case a user has signed in with a different provider
     *
     * @param externalUserId user id from external provider
     * @param email          user email
     * @return a BaseUserDTO if user exists, null otherwise
     * @throws com.danielagapov.spawn.Exceptions.IncorrectProviderException if user exists but with different provider
     */
    Optional<AuthResponseDTO> getUserIfExistsbyExternalId(String externalUserId, String email);

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

    /**
     * Performs comprehensive cleanup of orphaned OAuth data that can occur during concurrent operations.
     * This method should be called when data inconsistencies are detected during OAuth flows.
     * 
     * @param email The email to check for orphaned data
     * @param externalUserId The external user ID to check for orphaned mappings
     * @return true if cleanup was performed, false if no cleanup was needed
     */
    boolean performDataConsistencyCleanup(String email, String externalUserId);

    boolean isOAuthUser(UUID userId);

    OAuthProvider getOAuthProvider(UUID userId);
}
