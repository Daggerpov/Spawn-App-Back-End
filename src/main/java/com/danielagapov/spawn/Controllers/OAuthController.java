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

    // full path: /api/v1/oauth/google/sign-in
    @RequestMapping("google/sign-in")
    public ResponseEntity<AbstractUserDTO> googleSignIn(@AuthenticationPrincipal OAuth2User principal) {
        try {
            AbstractUserDTO verifiedUserDTO = oauthService.verifyUser(principal);
            return verifiedUserDTO instanceof UserDTO ? ResponseEntity.ok().body(verifiedUserDTO) : ResponseEntity.created(URI.create("api/v1/users")).body(verifiedUserDTO);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // full path: /api/v1/oauth/sign-in?sub=su
    @GetMapping("sign-in?sub=sub")
    public ResponseEntity<FullUserDTO> signIn(@RequestParam("sub") String sub) {
        try {
            return ResponseEntity.ok().body(oauthService.getUserIfExistsbyExternalId(sub));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("make-user")
    public ResponseEntity<UserDTO> makeUser(@RequestParam("user") UserDTO userDTO, @RequestParam("id") String id, @RequestParam("pfp") byte[] file) {
        try {
           UserDTO user = oauthService.makeUser(userDTO, id, file);
           return ResponseEntity.ok().body(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
