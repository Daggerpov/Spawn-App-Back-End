package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface IOAuthService {

    /**
     * Verifies whether user has an existing account or must create on
     * @param user => user to be verified
     * @return either TempUserDTO or UserDTO
     */
    AbstractUserDTO verifyUser(OAuth2User user);

    /**
     * Given a new user dto, creates a new account which means saving the user info and their external id mapping
     * @param user => new user to save
     * @param externalUserId => user id from external provider
     * @param profilePicture => byte arr of user's pfp
     * @param provider => oauth provider that the new user used to sign in with
     * @return FullUserDTO of the newly created user
     */
    // TODO: refactor to return UserDTO instead of Full since the new user won't have friends/events anyway
    FullUserDTO makeUser(UserDTO user, String externalUserId, byte[] profilePicture, OAuthProvider provider);

    /**
     * Given an external user id from an oauth provider, check whether it belongs it a user account.
     * First tries to find user by external id, then by email in case a user has signed in with a different provider
     * @param externalUserId => user id from external provider
     * @param email => user email
     * @return a FullUserDTO if user exists, null otherwise
     */
    FullUserDTO getUserIfExistsbyExternalId(String externalUserId, String email);
}
