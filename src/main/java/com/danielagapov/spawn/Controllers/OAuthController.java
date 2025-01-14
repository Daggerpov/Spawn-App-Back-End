package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("api/v1/oauth")
public class OAuthController {
    private final IOAuthService oauthService;

    public OAuthController(IOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    // full path: /api/v1/oauth/google/sign-in
    @RequestMapping("google/sign-in")
    public ResponseEntity<UserDTO> googleSignIn(@AuthenticationPrincipal OAuth2User principal) {
        try {
            UserDTO verifiedUserDTO = oauthService.verifyUser(principal);
            return ResponseEntity.ok().body(verifiedUserDTO);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
