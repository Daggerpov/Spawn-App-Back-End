package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.FullUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.FieldAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.Token.BadTokenException;
import com.danielagapov.spawn.Exceptions.Token.TokenNotFoundException;
import com.danielagapov.spawn.Services.Auth.IAuthService;
import com.danielagapov.spawn.Services.Email.IEmailService;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;


@RestController()
@RequestMapping("api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final IOAuthService oauthService;
    private final IJWTService jwtService;
    private final ILogger logger;
    private final IAuthService authService;
    private final IEmailService emailService;

    /**
     * This method is meant to check whether an externally signed-in user through either Google or Apple
     * already has an existing `User` created within spawn, given their external user id, which we check
     * against our mappings of internal ids to external ones.
     * <p>
     * If the user is already saved within Spawn -> we return its `FullUserDTO`. Otherwise, null.
     */
    // full path: /api/v1/auth/sign-in?externalUserId=externalUserId&email=email
    @GetMapping("sign-in")
    public ResponseEntity<FullUserDTO> signIn(@RequestParam("externalUserId") String externalUserId, @RequestParam(value = "email", required = false) String email) {
        try {
            logger.log(String.format("Received sign-in request: {externalUserId: %s, email: %s}", externalUserId, email));
            FullUserDTO userDTO = oauthService.getUserIfExistsbyExternalId(externalUserId, email);
            if (userDTO != null) {
                HttpHeaders headers = makeHeadersForTokens(userDTO.getUsername());
                return ResponseEntity.ok().headers(headers).body(userDTO);
            }
            return ResponseEntity.ok().body(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }


    /**
     * This method creates a user, given a `UserDTO` from mobile, which can be constructed through the email
     * given through Google, Apple, or email/pass authentication + attributes input either by default through
     * these providers, such as full name & pfp, or supplied by the user (i.e. overwritten by provider, or new).
     * <p>
     * For profile pictures specifically, the userCreationDTO.profilePicture attribute will supply it
     * to overwrite/write the profile picture to the user, by saving it to the S3Service
     * <p>
     * Another argument is the `externalUserId`, which is a unique identifier for a user used by the external provider chosen
     */
    // full path: /api/v1/auth/make-user
    @PostMapping("make-user")
    public ResponseEntity<FullUserDTO> makeUser(@RequestBody UserCreationDTO userCreationDTO,
                                                @RequestParam("externalUserId") String externalUserId,
                                                @RequestParam(value = "provider") OAuthProvider provider) {
        try {
            logger.log(String.format("Received make-user request: {userDTO: %s, externalUserId: %s, provider: %s}",
                    userCreationDTO, externalUserId, provider));
            FullUserDTO user = oauthService.createUser(userCreationDTO, externalUserId, provider);
            HttpHeaders headers = makeHeadersForTokens(userCreationDTO.getUsername());
            return ResponseEntity.ok().headers(headers).body(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // full path: /api/v1/auth/refresh-token
    @PostMapping("refresh-token")
    public ResponseEntity<String> refreshToken(HttpServletRequest request) {
        try {
            logger.log(String.format("Refresh token request received: {token: %s}",
                    request.getHeader("Authorization")));
            HttpHeaders headers = new HttpHeaders();
            String token = jwtService.refreshAccessToken(request);
            headers.add("Authorization", "Bearer " + token);
            return ResponseEntity.ok().headers(headers).body(token);
        } catch (TokenNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No authorization token found");
        } catch (BadTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad or expired token");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // full path: /api/v1/auth/register
    @PostMapping("register")
    public ResponseEntity<UserDTO> register(@RequestBody() AuthUserDTO authUserDTO) {
        try {
            logger.log(String.format("Account registration request received: {user: %s}", authUserDTO));
            UserDTO newUserDTO = authService.registerUser(authUserDTO);
            HttpHeaders headers = makeHeadersForTokens(newUserDTO.getUsername());
            logger.log(String.format("User successfully registered: {user: %s}", newUserDTO));
            return ResponseEntity.ok().headers(headers).body(newUserDTO);
        } catch (FieldAlreadyExistsException fae) {
            logger.log(fae.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception e) {
            logger.log("Error registering in user: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/auth/login
    @PostMapping("login")
    public ResponseEntity<AbstractUserDTO> login(@RequestBody AuthUserDTO authUserDTO) {
        try {
            logger.log(String.format("Login request received: {user: %s}", authUserDTO));
            FullUserDTO existingUserDTO = authService.loginUser(authUserDTO);
            HttpHeaders headers = makeHeadersForTokens(existingUserDTO.getUsername());
            return ResponseEntity.ok().headers(headers).body(existingUserDTO);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.log(String.format("Error logging in user: {user: %s}. Error: %s", authUserDTO, e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/auth/verify-email?token=<email-token>
    @GetMapping("verify-email")
    public ModelAndView verifyEmail(@RequestParam("token") String emailToken) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("verifyAccountPage");
        try {
            logger.log("Verify email request received");
            boolean isVerified = authService.verifyEmail(emailToken);
            String status = isVerified ? "success" : "expired";
            modelAndView.addObject("status", status);
            modelAndView.setStatus(HttpStatus.OK);
            return modelAndView;
        } catch (Exception e) {
            logger.log("Unexpected error while verifying email: " + e.getMessage());
            modelAndView.addObject("status", "error");
            modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            return modelAndView;
        }
    }

    /* ------------------------------ HELPERS ------------------------------ */

    @Deprecated(since = "For testing purposes")
    @GetMapping("test-email")
    public ResponseEntity<String> email() {
        try {
            logger.log("Email request received");
            emailService.sendEmail("spawnappmarketing@gmail.com", "Test Email", "This is a test email sent programmatically.");
            return ResponseEntity.ok().body("Email sent");
        } catch (MessagingException e) {
            logger.log("Messaging Exception: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Messaging Exception: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Helper method to call access/refresh token-generating methods and place them in the appropriate
     * HTTP headers
     */
    private HttpHeaders makeHeadersForTokens(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.generateAccessToken(username));
        headers.set("X-Refresh-Token", jwtService.generateRefreshToken(username));
        return headers;
    }
}
