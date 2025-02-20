package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController()
@RequestMapping("api/v1/oauth")
public class OAuthController {
    private final IOAuthService oauthService;
    private final ILogger logger;

    public OAuthController(IOAuthService oauthService, ILogger logger) {
        this.oauthService = oauthService;
        this.logger = logger;
    }

    /**
     * @param principal the google oauth response
     * @return either a `UserDTO` if they're verified to already have been a Spawn user, or
     * a newly-created user if they weren't previously a Spawn user
     */
    // full path: /api/v1/oauth/google/sign-in
    @RequestMapping("google/sign-in")
    @Deprecated(since = "We no longer use this to sign in, since our authentication is done through mobile")
    public ResponseEntity<AbstractUserDTO> googleSignIn(@AuthenticationPrincipal OAuth2User principal) {
        try {
            AbstractUserDTO verifiedUserDTO = oauthService.verifyUser(principal);
            return verifiedUserDTO instanceof UserDTO ? ResponseEntity.ok().body(verifiedUserDTO) : ResponseEntity.created(URI.create("api/v1/users")).body(verifiedUserDTO);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * This method is meant to check whether an externally signed-in user through either Google or Apple
     * already has an existing `User` created within spawn, given their external user id, which we check
     * against our mappings of internal ids to external ones.
     * <p>
     * If the user is already saved within Spawn -> we return its `FullUserDTO`. Otherwise, null.
     */
    // full path: /api/v1/oauth/sign-in?externalUserId=externalUserId&email=email
    @GetMapping("sign-in")
    public ResponseEntity<FullUserDTO> signIn(@RequestParam("externalUserId") String externalUserId, @RequestParam(value = "email", required = false) String email) {
        try {
            logger.log(String.format("Received sign-in request: {externalUserId: %s, email: %s}", externalUserId, email));
            return ResponseEntity.ok().body(oauthService.getUserIfExistsbyExternalId(externalUserId, email));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * This method creates a user, given a `UserDTO` from mobile, which can be constructed through the email
     * given through Google, Apple, or by default through
     * these providers, such as full name & pfp, or supplied by the user (i.e. overwritten by provider, or new).
     * <p>
     * For profile pictures specifically, there's an optional argument, `profilePicture`, which will take a raw
     * byte file to overwrite/write the profile picture to the user, by saving it to the S3Service
     * <p>
     * Another argument is the `externalUserId`, which is a unique identifier for a user used by the external provider chosen
     */
    // full path: /api/v1/oauth/make-user
    @PostMapping("make-user")
    public ResponseEntity<FullUserDTO> makeUser(@RequestBody UserDTO userDTO, @RequestParam(value = "externalUserId") String externalUserId, @RequestParam(value = "profilePicture", required = false) byte[] profilePicture, @RequestParam(value = "provider") OAuthProvider provider) {
        try {
            logger.log(String.format("Received make-user request: {userDTO: %s, externalUserId: %s, provider: %s}", userDTO, externalUserId, provider));
            FullUserDTO user = oauthService.makeUser(userDTO, externalUserId, profilePicture, provider);
            return ResponseEntity.ok().body(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
