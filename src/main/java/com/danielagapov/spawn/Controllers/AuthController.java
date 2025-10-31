package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.CheckEmailVerificationRequestDTO;
import com.danielagapov.spawn.DTOs.EmailVerificationResponseDTO;
import com.danielagapov.spawn.DTOs.OAuthRegistrationDTO;
import com.danielagapov.spawn.DTOs.SendEmailVerificationRequestDTO;
import com.danielagapov.spawn.DTOs.User.*;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.*;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.Token.BadTokenException;
import com.danielagapov.spawn.Exceptions.Token.TokenNotFoundException;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Services.Auth.IAuthService;
import com.danielagapov.spawn.Services.Email.IEmailService;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.ErrorResponse;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;
import java.util.UUID;


@RestController()
@RequestMapping("api/v1/auth")
@AllArgsConstructor
public final class AuthController {
    private final IOAuthService oauthService;
    private final IJWTService jwtService;
    private final ILogger logger;
    private final IAuthService authService;
    private final IEmailService emailService;
    private final IUserService userService;

    /**
     * This method is meant to check whether an externally signed-in user through either Google or Apple
     * already has an existing `User` created within spawn, given their external user id, which we check
     * against our mappings of internal ids to external ones.
     * <p>
     * If the user is already saved within Spawn -> we return its `BaseUserDTO`. Otherwise, null.
     */
    // full path: /api/v1/auth/sign-in?externalUserId=externalUserId&email=email
    @GetMapping("sign-in")
    public ResponseEntity<?> signIn(
            @RequestParam(value = "idToken", required = true) String idToken,
            @RequestParam(value = "provider", required = true) OAuthProvider provider,
            @RequestParam(value = "email", required = false) String email)
    {
        try {
            Optional<AuthResponseDTO> optionalDTO = oauthService.signInUser(idToken, email, provider);
            
            if (optionalDTO.isPresent()) {
                AuthResponseDTO authResponseDTO = optionalDTO.get();
                // Use User object for token generation to handle null usernames
                User user = userService.getUserEntityById(authResponseDTO.getUser().getId());
                HttpHeaders headers = authService.makeHeadersForTokens(user);
                return ResponseEntity.ok().headers(headers).body(authResponseDTO);
            }
            // User doesn't exist - return 404 instead of 200 with null body
            logger.info("User not found during OAuth sign-in - returning 404");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        } catch (IncorrectProviderException e) {
            logger.error("Incorrect provider error during sign-in: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (TokenExpiredException e) {
            logger.error("Token expired during sign-in: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        } catch (OAuthProviderUnavailableException e) {
            logger.error("OAuth provider unavailable during sign-in: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(e.getMessage()));
        } catch (BaseNotFoundException e) {
            logger.error("Entity not found during sign-in: " + e.entityType);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.entityType);
        } catch (SecurityException e) {
            logger.error("Security error during sign-in: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during sign-in: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse(e.getMessage()));
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
    public ResponseEntity<BaseUserDTO> makeUser(
            @RequestBody UserCreationDTO userCreationDTO,
            @RequestParam(value = "idToken") String idToken,
            @RequestParam(value = "provider") OAuthProvider provider) {
        try {
            BaseUserDTO user = oauthService.createUserFromOAuth(userCreationDTO, idToken, provider);
            HttpHeaders headers = authService.makeHeadersForTokens(userCreationDTO.getUsername());
            return ResponseEntity.ok().headers(headers).body(user);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request during user creation: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (SecurityException e) {
            logger.warn("Security error during user creation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        } catch (Exception e) {
            logger.error("Unexpected error during user creation: " + e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // full path: /api/v1/auth/refresh-token
    @PostMapping("refresh-token")
    public ResponseEntity<String> refreshToken(HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String token = jwtService.refreshAccessToken(request);
            headers.add("Authorization", "Bearer " + token);
            return ResponseEntity.ok().headers(headers).body(token);
        } catch (TokenNotFoundException e) {
            logger.error("No authorization token found for refresh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No authorization token found");
        } catch (BadTokenException e) {
            logger.error("Bad or expired token for refresh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad or expired token");
        } catch (BaseNotFoundException e) {
            logger.error("Entity not found during token refresh: " + e.entityType);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Entity not found: " + e.entityType);
        } catch (Exception e) {
            logger.error("Unexpected error during token refresh: " + e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // full path: /api/v1/auth/register
    @PostMapping("register")
    public ResponseEntity<UserDTO> register(@RequestBody() AuthUserDTO authUserDTO) {
        try {
            UserDTO newUserDTO = authService.registerUser(authUserDTO);
            HttpHeaders headers = authService.makeHeadersForTokens(newUserDTO.getUsername());
            return ResponseEntity.ok().headers(headers).body(newUserDTO);
        } catch (FieldAlreadyExistsException fae) {
            logger.warn("Registration failed - field already exists: " + fae.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (BaseNotFoundException e) {
            logger.error("Entity not found during registration: " + e.entityType);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            logger.error("Error registering user: " + authUserDTO.getUsername() + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/auth/login
    @PostMapping("login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        try {
            AuthResponseDTO authResponseDTO = authService.loginUser(loginDTO.getUsernameOrEmail(), loginDTO.getPassword());
            HttpHeaders headers = authService.makeHeadersForTokens(authResponseDTO.getUser().getUsername());
            return ResponseEntity.ok().headers(headers).body(authResponseDTO);
        } catch (BadCredentialsException e) {
            logger.warn("Login failed - bad credentials for user: " + loginDTO.getUsernameOrEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (BaseNotFoundException e) {
            logger.error("Entity not found during login: " + e.entityType);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error logging in user:  Error: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/auth/verify-email?token=<email-token>
    @GetMapping("verify-email")
    public ModelAndView verifyEmail(@RequestParam("token") String emailToken) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("verifyAccountPage");
        try {
            boolean isVerified = authService.verifyEmail(emailToken);
            String status = isVerified ? "success" : "expired";
            modelAndView.addObject("status", status);
            modelAndView.setStatus(HttpStatus.OK);
            return modelAndView;
        } catch (BaseNotFoundException e) {
            logger.error("Error verifying email: " + e.getMessage() + ", entity type: " + e.entityType);
            modelAndView.addObject("status", "not_found");
            modelAndView.addObject("entityType", e.entityType);
            modelAndView.setStatus(HttpStatus.NOT_FOUND);
            return modelAndView;
        } catch (Exception e) {
            logger.error("Unexpected error while verifying email: " + e.getMessage());
            modelAndView.addObject("status", "error");
            modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            return modelAndView;
        }
    }

    // New registration flow endpoints

    // full path: /api/v1/auth/register/oauth
    @PostMapping("register/oauth")
    public ResponseEntity<?> registerViaOAuth(@RequestBody OAuthRegistrationDTO registration) {
        try {
            AuthResponseDTO user = authService.registerUserViaOAuth(registration);
            // Use User object for token generation to handle null usernames
            User userEntity = userService.getUserEntityById(user.getUser().getId());
            HttpHeaders headers = authService.makeHeadersForTokens(userEntity);
            return ResponseEntity.ok().headers(headers).body(user);
        } catch (AccountAlreadyExistsException e) {
            logger.warn("OAuth registration failed - account already exists: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (FieldAlreadyExistsException e) {
            logger.warn("OAuth registration failed - field already exists: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during OAuth registration: " + e.getMessage());
            // Try graceful handling if registration fails
            try {
                AuthResponseDTO gracefulUser = authService.handleOAuthRegistrationGracefully(registration, e);
                if (gracefulUser != null) {
                    User userEntity = userService.getUserEntityById(gracefulUser.getUser().getId());
                    HttpHeaders headers = authService.makeHeadersForTokens(userEntity);
                    logger.info("OAuth registration succeeded via graceful handling");
                    return ResponseEntity.ok().headers(headers).body(gracefulUser);
                } else {
                    logger.warn("Graceful handling returned null - attempting final fallback check");
                    
                    // Final fallback: try to sign in the user if they already exist
                    try {
                        Optional<AuthResponseDTO> signInUser = oauthService.signInUser(
                            registration.getIdToken(), 
                            registration.getEmail(), 
                            registration.getProvider()
                        );
                        
                        if (signInUser.isPresent()) {
                            User userEntity = userService.getUserEntityById(signInUser.get().getUser().getId());
                            HttpHeaders headers = authService.makeHeadersForTokens(userEntity);
                            logger.info("OAuth registration succeeded via final fallback sign-in");
                            return ResponseEntity.ok().headers(headers).body(signInUser.get());
                        }
                    } catch (Exception fallbackEx) {
                        logger.warn("Final fallback sign-in also failed: " + fallbackEx.getMessage());
                    }
                }
            } catch (Exception gracefulException) {
                logger.error("Graceful handling also failed: " + gracefulException.getMessage());
            }
            
            // If all recovery attempts failed, return detailed error
            logger.error("All OAuth registration attempts failed for email: " + registration.getEmail());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Registration failed. Please try again or contact support if the issue persists."));
        }
    }

    // full path: /api/v1/auth/register/send
    @PostMapping("register/verification/send")
    public ResponseEntity<?> sendEmailVerificationForRegistration(@RequestBody SendEmailVerificationRequestDTO request) {
        try {
            EmailVerificationResponseDTO response = authService.sendEmailVerificationCodeForRegistration(request.getEmail());
            return ResponseEntity.ok().body(response);
        } catch (FieldAlreadyExistsException e) {
            logger.warn("Email already exists during registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error sending email verification for registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to send verification code"));
        }
    }

    // full path: /api/v1/auth/register/verification/check
    @PostMapping("register/verification/check")
    public ResponseEntity<?> verifyEmailAndCreateUser(@RequestBody CheckEmailVerificationRequestDTO request) {
        try {
            AuthResponseDTO authResponseDTO = authService.checkEmailVerificationCode(request.getEmail(), request.getVerificationCode());
            // Use User object for token generation to handle users with null usernames
            HttpHeaders headers = authService.makeHeadersForTokens(authResponseDTO.getUser().getUsername());
            return ResponseEntity.ok().headers(headers).body(authResponseDTO);
        } catch (EmailVerificationException e) {
            logger.warn("Email verification failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
        } catch (FieldAlreadyExistsException e) {
            logger.warn("User creation failed - field already exists: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error verifying email and creating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to verify email and create user"));
        }
    }

    // full path: /api/v1/auth/change-password
    @PostMapping("change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeDTO passwordChangeDTO, HttpServletRequest request) {
        try {
            // Extract username from JWT token
            final String authHeader = request.getHeader("Authorization");
            final String token = authHeader.substring(7);
            final String username = jwtService.extractUsername(token);
            
            boolean success = authService.changePassword(
                username, 
                passwordChangeDTO.getCurrentPassword(), 
                passwordChangeDTO.getNewPassword()
            );
            
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                logger.warn("Password change failed for user: " + username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Current password is incorrect"));
            }
        } catch (Exception e) {
            logger.error("Error changing password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to change password"));
        }
    }

    // full path: /api/v1/auth/quick-sign-in
    @GetMapping("quick-sign-in")
    public ResponseEntity<?> quickSignIn(HttpServletRequest request) {
        try {
            AuthResponseDTO authResponse = authService.getUserByToken(request.getHeader("Authorization").substring(7));
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Error while performing quick sign-in"));
        }
    }

    // full path: /api/v1/auth/user/details
    @PostMapping("user/details")
    public ResponseEntity<?> updateUserDetails(@RequestBody UpdateUserDetailsDTO dto) {
        try {
            BaseUserDTO updatedUser = authService.updateUserDetails(dto);
            // Use User object for token generation to handle cases where username was just set
            User user = userService.getUserEntityById(updatedUser.getId());
            HttpHeaders headers = authService.makeHeadersForTokens(user);
            return ResponseEntity.ok().headers(headers).body(updatedUser);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for update: " + dto.getId() + ", entity type: " + e.entityType);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        } catch (FieldAlreadyExistsException e) {
            logger.warn("Username already exists: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating user details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to update user details"));
        }
    }

    // full path: /api/v1/auth/complete-contact-import/{userId}
    @PostMapping("complete-contact-import/{userId}")
    public ResponseEntity<?> completeContactImport(@PathVariable UUID userId) {
        try {
            BaseUserDTO updatedUser = authService.completeContactImport(userId);
            return ResponseEntity.ok(updatedUser);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for contact import completion: " + userId + ", entity type: " + e.entityType);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        } catch (Exception e) {
            logger.error("Error completing contact import for user: " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to complete contact import"));
        }
    }

    // full path: /api/v1/auth/accept-tos/{userId}
    @PostMapping("accept-tos/{userId}")
    public ResponseEntity<?> acceptTermsOfService(@PathVariable UUID userId) {
        try {
            BaseUserDTO updatedUser = authService.acceptTermsOfService(userId);
            return ResponseEntity.ok(updatedUser);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for TOS acceptance: " + userId + ", entity type: " + e.entityType);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User not found"));
        } catch (Exception e) {
            logger.error("Error accepting Terms of Service for user: " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Failed to accept Terms of Service"));
        }
    }

    /* ------------------------------ HELPERS ------------------------------ */

    @Deprecated(since = "For testing purposes")
    @GetMapping("test-email")
    public ResponseEntity<String> email() {
        try {
            emailService.sendEmail("spawnappmarketing@gmail.com", "Test Email", "This is a test email sent programmatically.");
            return ResponseEntity.ok().body("Email sent");
        } catch (MessagingException e) {
            logger.error("Messaging Exception: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Messaging Exception: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.internalServerError().body("Internal Server Error: " + e.getMessage());
        }
    }
}
