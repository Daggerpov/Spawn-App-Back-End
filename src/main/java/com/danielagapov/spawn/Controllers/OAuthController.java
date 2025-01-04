package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController()
@RequestMapping("api/v1/oauth")
public class OAuthController {
    private final IUserService userService;

    public OAuthController(IUserService userService) {
        this.userService = userService;
    }

    // full path: login/oauth2/code/google
    // if user does not exist, create user account
    // if user does exist, do nothing ?

    /**
     * attributes to grab:
     * - given_name
     * - family_name
     * - picture
     * - email
     *
     * @param principal
     * @return
     */
    @RequestMapping("google/sign-in")
    public ResponseEntity<String> googleSignIn(@AuthenticationPrincipal OAuth2User principal) {
        String given_name = principal.getAttribute("given_name");
        String family_name = principal.getAttribute("family_name");
        String picture = principal.getAttribute("picture"); // TODO: may need to change once S3 is set
        String email = principal.getAttribute("email"); // to be used as username
        String sub = principal.getAttribute("sub"); // sub is a unique identifier for google accounts

        if (sub == null) return ResponseEntity.internalServerError().body("Invalid sub"); // shouldn't happen but to be safe

        UUID uuid = UUID.nameUUIDFromBytes(sub.getBytes());
        System.out.println(uuid);
        UserDTO userDTO = new UserDTO(uuid, email, given_name, family_name, null, picture, null);
        try {
            UserDTO verifiedUserDTO = userService.verifyUserOAuth(userDTO, uuid); // TODO: handle case where email exists already; seems to be affected by @GeneratedValue in Model.User
            return ResponseEntity.ok().body(verifiedUserDTO.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error verifying user with Google OAuth");
        }
    }
}
