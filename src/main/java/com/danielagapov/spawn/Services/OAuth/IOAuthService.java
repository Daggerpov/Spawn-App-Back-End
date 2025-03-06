package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.User.FullUserDTO;
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
     * @return FullUserDTO of the newly created user
     */
    // TODO: refactor to return UserDTO instead of Full since the new user won't have friends/events anyway
    FullUserDTO makeUser(UserDTO user, String externalUserId, byte[] profilePicture, OAuthProvider provider);


    /**
     * @param userCreationDTO given from mobile, containing the profile picture data within it
     * @param externalUserId  externalUserId, from Google or Apple
     * @param provider        provider indicating Google or Apple
     * @return returns back the fully-created user, after it goes through the `makeUser()` method
     */
    FullUserDTO createUser(UserCreationDTO userCreationDTO, String externalUserId, OAuthProvider provider);

    /**
     * Given an external user id from an oauth provider, check whether it belongs it a user account.
     * First tries to find user by external id, then by email in case a user has signed in with a different provider
     *
     * @param externalUserId user id from external provider
     * @param email          user email
     * @return a FullUserDTO if user exists, null otherwise
     */
    Optional<FullUserDTO> getUserIfExistsbyExternalId(String externalUserId, String email);
}
