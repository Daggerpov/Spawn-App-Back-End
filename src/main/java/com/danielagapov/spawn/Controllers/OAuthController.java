package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
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

    public OAuthController(IOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    /**
     * 
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
     * 
     * If the user is already saved within Spawn -> we return its `FullUserDTO`. Otherwise, null.
     */
    // full path: /api/v1/oauth/sign-in?externalUserId=externalUserId
    @GetMapping("sign-in")
    public ResponseEntity<FullUserDTO> signIn(@RequestParam("externalUserId") String externalUserId) {
        try {
            return ResponseEntity.ok().body(oauthService.getUserIfExistsbyExternalId(externalUserId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * This method creates a user, given a `UserDTO` from mobile, which can be constructed through the email 
     * given through Google, Apple, or email/pass authentication + attributes input either by default through 
     * these providers, such as full name & pfp, or supplied by the user (i.e. overwritten by provider, or new).
     * 
     * For profile pictures specifically, there's an optional argument, `profilePicture`, which will take a raw 
     * byte file to overwrite/write the profile picture to the user, by saving it to the S3Service
     * 
     * Another argument is the `externalUserId`, which should be optional, since a user could be created 
     * without the use of an external provider (i.e. Google or Apple), through our own email/pass authentication.
     * 
     */
    // full path: /api/v1/oauth/make-user
    @PostMapping("make-user")
    public ResponseEntity<UserDTO> makeUser(@RequestBody UserDTO userDTO, @RequestParam("externalUserId") String externalUserId, @RequestParam(value="profilePicture", required=false) byte[] profilePicture) {
        try {
           UserDTO user = oauthService.makeUser(userDTO, externalUserId, profilePicture);
           return ResponseEntity.ok().body(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
