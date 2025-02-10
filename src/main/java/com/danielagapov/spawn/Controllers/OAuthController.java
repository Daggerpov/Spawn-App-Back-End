package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController()
@RequestMapping("api/v1/oauth")
public class OAuthController {
    private final IOAuthService oauthService;
    private final IJWTService jwtService;
    private final ILogger logger;

    public OAuthController(IOAuthService oauthService, IJWTService jwtService, ILogger logger) {
        this.oauthService = oauthService;
        this.jwtService = jwtService;
        this.logger = logger;
    }


    /**
     * This method is meant to check whether an externally signed-in user through either Google or Apple
     * already has an existing `User` created within spawn, given their external user id, which we check 
     * against our mappings of internal ids to external ones. 
     * 
     * If the user is already saved within Spawn -> we return its `FullUserDTO`. Otherwise, null.
     */
    // full path: /api/v1/oauth/sign-in?externalUserId=externalUserId&email=email
    @GetMapping("sign-in")
    public ResponseEntity<FullUserDTO> signIn(@RequestParam("externalUserId") String externalUserId, @RequestParam("email") String email) {
        try {
            logger.log(String.format("Received sign-in request: {externalUserId: %s, email: %s}", externalUserId, email));
            FullUserDTO userDTO = oauthService.getUserIfExistsbyExternalId(externalUserId, email);
            HttpHeaders headers = makeHeaders(userDTO.username());
            return ResponseEntity.ok().headers(headers).body(userDTO);
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
    public ResponseEntity<FullUserDTO> makeUser(@RequestBody UserDTO userDTO, @RequestParam("externalUserId") String externalUserId, @RequestParam(value="profilePicture", required=false) byte[] profilePicture, @RequestParam(value = "provider", required = false) OAuthProvider provider) {
        try {
            logger.log(String.format("Received make-user request: {userDTO: %s, externalUserId: %s, provider: %s}", userDTO, externalUserId, provider));
            FullUserDTO user = oauthService.makeUser(userDTO, externalUserId, profilePicture, provider);
            HttpHeaders headers = makeHeaders(userDTO.username());
            return ResponseEntity.ok().headers(headers).body(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    private HttpHeaders makeHeaders(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.generateToken(username));
        return headers;
    }
}
