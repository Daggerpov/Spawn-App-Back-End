package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;

import java.util.Optional;

public interface IOAuthService {


    /**
     * Given a new user dto, creates a new account which means saving the user info and their external id mapping
     *
     * @param user           new user to save
     * @param externalUserId user id from external provider
     * @param profilePicture byte arr of user's pfp
     * @param provider       oauth provider that the new user used to sign in with
     * @return BaseUserDTO of the newly created user
     */
    // TODO: refactor to return UserDTO instead of Full since the new user won't have friends/events anyway
    BaseUserDTO makeUser(UserDTO user, String externalUserId, byte[] profilePicture, OAuthProvider provider);

    /**
     * Creates a user account using Google ID token for authentication
     * 
     * @param user           new user to save
     * @param idToken        Google ID token for authentication and verification
     * @param profilePicture byte arr of user's pfp 
     * @return BaseUserDTO of the newly created user
     */
    BaseUserDTO makeUserWithGoogleToken(UserDTO user, String idToken, byte[] profilePicture);

    /**
     * @param userCreationDTO given from mobile, containing the profile picture data within it
     * @param externalUserId  externalUserId, from Google or Apple
     * @param provider        provider indicating Google or Apple
     * @return returns back the fully-created user, after it goes through the `makeUser()` method
     */
    BaseUserDTO createUser(UserCreationDTO userCreationDTO, String externalUserId, OAuthProvider provider);
    
    /**
     * Creates a user account using Google ID token for authentication
     * 
     * @param userCreationDTO given from mobile, containing profile picture data
     * @param idToken        Google ID token for authentication and verification
     * @return returns the fully-created user
     */
    BaseUserDTO createUserWithGoogleToken(UserCreationDTO userCreationDTO, String idToken);

    /**
     * Given an external user id from an oauth provider, check whether it belongs it a user account.
     * First tries to find user by external id, then by email in case a user has signed in with a different provider
     *
     * @param externalUserId user id from external provider
     * @param email          user email
     * @return a BaseUserDTO if user exists, null otherwise
     */
    Optional<BaseUserDTO> getUserIfExistsbyExternalId(String externalUserId, String email);
    
    /**
     * Verifies a Google ID token and returns the user if they exist
     * 
     * @param idToken Google ID token for authentication and verification
     * @param email user email
     * @return a BaseUserDTO if user exists, null otherwise
     */
    Optional<BaseUserDTO> getUserIfExistsByGoogleToken(String idToken, String email);
    
    /**
     * Verifies a Google ID token and extracts the subject (user ID)
     * 
     * @param idToken Google ID token to verify
     * @return the subject (user ID) extracted from the token
     */
    String verifyGoogleIdToken(String idToken);

    /**
     * Creates a user from either Google ID token or Apple external user ID
     * 
     * @param userCreationDTO DTO containing user creation details
     * @param externalUserId External user ID for Apple authentication (optional)
     * @param idToken Google ID token (optional)
     * @param provider OAuth provider (required for Apple)
     * @return Created or existing user BaseUserDTO
     * @throws IllegalArgumentException when required parameters are missing
     * @throws SecurityException when token validation fails
     */
    BaseUserDTO createUserFromOAuth(UserCreationDTO userCreationDTO, String externalUserId, String idToken, OAuthProvider provider);
}
