package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.EmailVerificationResponseDTO;
import com.danielagapov.spawn.DTOs.OAuthRegistrationDTO;
import com.danielagapov.spawn.DTOs.User.*;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Enums.UserField;
import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.*;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.EmailVerification;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IEmailVerificationRepository;
import com.danielagapov.spawn.Services.Email.IEmailService;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import com.danielagapov.spawn.Services.S3.S3Service;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.LoggingUtils;
import com.danielagapov.spawn.Util.VerificationCodeGenerator;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService implements IAuthService {
    private final IUserService userService;
    private final IJWTService jwtService;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ILogger logger;
    private final IOAuthService oauthService;
    private final IEmailVerificationRepository emailVerificationRepository;


    @Override
    public UserDTO registerUser(AuthUserDTO authUserDTO) throws FieldAlreadyExistsException {
        logger.info("Attempting to register new user with username: " + authUserDTO.getUsername());
        checkIfUniqueCredentials(authUserDTO);
        try {
            UserDTO userDTO = createAndSaveUser(authUserDTO);
            User user = UserMapper.toEntity(userDTO);
            logger.info("User registered successfully: " + LoggingUtils.formatUserInfo(user));
            createEmailTokenAndSendEmail(authUserDTO);
            return userDTO;
        } catch (Exception e) {
            logger.error("Unexpected error while registering user with username: " + authUserDTO.getUsername() + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public BaseUserDTO loginUser(String usernameOrEmail, String password) {
        String username;
        final String errorMsg = "Incorrect username, email, or password";

        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username, email, and password must be provided");
        }

        User user = null;
        if (usernameOrEmail.contains("@")) { // This is an email
            user = userService.getUserByEmail(usernameOrEmail);
            if (user == null) {
                throw new BadCredentialsException(errorMsg);
            }

            username = user.getUsername();
        } else { // This is a username
            username = usernameOrEmail;
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        if (authentication.isAuthenticated()) {
            String authenticatedUsername = ((UserDetails) authentication.getPrincipal()).getUsername();

            if (user == null) {
                user = userService.getUserEntityByUsername(authenticatedUsername);
            }

            return UserMapper.toDTO(user);
        }
        throw new BadCredentialsException(errorMsg);
    }

    @Override
    public boolean verifyEmail(String token) {
        try {
            if (jwtService.isValidEmailToken(token)) {
                // The email token is valid so mark this user as verified user in database
                final String username = jwtService.extractUsername(token);
                logger.info("Verifying email for user with username: " + username);
                
                User user = userService.getUserEntityByUsername(username);
                userService.saveEntity(user);
                
                logger.info("Email verified successfully for user: " + LoggingUtils.formatUserInfo(user));
                return true;
            }
            logger.warn("Invalid email verification token received");
            return false;
        } catch (Exception e) {
            logger.error("Error during email verification: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        try {
            logger.info("Attempting to change password for user: " + username);
            
            // Verify current password by attempting authentication
            try {
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, currentPassword)
                );
            } catch (BadCredentialsException e) {
                logger.warn("Current password verification failed for user: " + username);
                return false;
            }
            
            // Get user and update password
            User user = userService.getUserEntityByUsername(username);
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.saveEntity(user);
            
            logger.info("Password successfully changed for user: " + username);
            return true;
        } catch (Exception e) {
            logger.error("Error changing password for user: " + username + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public AuthResponseDTO getUserByToken(String token) {
        final String username = jwtService.extractUsername(token);
        User user = userService.getUserEntityByUsername(username);
        return UserMapper.toAuthResponseDTO(user);
    }

    @Override
    public BaseUserDTO updateUserDetails(UpdateUserDetailsDTO dto) {
        if (dto.getId() == null || dto.getUsername() == null || dto.getPhoneNumber() == null) {
            throw new IllegalArgumentException("User ID, username, and phone number cannot be null");
        }

        User user = userService.getUserEntityById(dto.getId());
        if (user == null) {
            throw new BaseNotFoundException(EntityType.User);
        }

        if (user.getStatus() != UserStatus.EMAIL_VERIFIED) {
            throw new RuntimeException("Cannot update user details before email is verified");
        }

        // Check for username uniqueness if changed
        if (!dto.getUsername().equals(user.getUsername())) {
            if (userService.existsByUsername(dto.getUsername())) {
                throw new FieldAlreadyExistsException("Username already exists", UserField.USERNAME);
            }
            user.setUsername(dto.getUsername());
            user.setName(dto.getUsername());
        }
        // Update phone number
        if (!userService.existsByPhoneNumber(dto.getPhoneNumber())) {
            user.setPhoneNumber(dto.getPhoneNumber());
        } else {
            throw new PhoneNumberAlreadyExistsException("Phone number already exists");
        }
        // Update password if provided
        if (dto.getPassword() != null && !dto.getPassword().isEmpty() && user.getPassword() == null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setStatus(UserStatus.USERNAME_AND_PHONE_NUMBER);
        userService.saveEntity(user);
        return UserMapper.toDTO(user);
    }


    @Override
    public AuthResponseDTO registerUserViaOAuth(OAuthRegistrationDTO registrationDTO) {
        String email = registrationDTO.getEmail();
        String idToken = registrationDTO.getIdToken();  // Changed from getExternalIdToken to getIdToken
        OAuthProvider provider = registrationDTO.getProvider();
        
        if (email == null && idToken == null) {
            throw new IllegalArgumentException("Email and idToken cannot be null for OAuth registration");
        }
        
        // Verify OAuth token and get external ID
        // Note: checkOAuthRegistration will handle incomplete users by deleting them
        String externalId = oauthService.checkOAuthRegistration(email, idToken, provider);
        
        // Check if user already exists and is ACTIVE - if so, redirect to sign-in behavior
        Optional<AuthResponseDTO> existingUser = oauthService.getUserIfExistsbyExternalId(externalId, email);
        if (existingUser.isPresent()) {
            AuthResponseDTO authResponse = existingUser.get();
            if (authResponse.getStatus() == null || authResponse.getStatus() == UserStatus.ACTIVE) {
                logger.info("ACTIVE user attempting to register - redirecting to sign-in flow: " + authResponse.getUser().getEmail());
                return authResponse;
            }
        }
        
        // Create verified user immediately for OAuth
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(externalId);
        newUser.setPhoneNumber(externalId);
        newUser.setName(externalId);
        newUser.setStatus(UserStatus.EMAIL_VERIFIED); // OAuth users are automatically verified
        newUser.setDateCreated(new Date());

        String profilePictureUrl = registrationDTO.getProfilePictureUrl();
        newUser.setProfilePictureUrlString(profilePictureUrl == null ? S3Service.getDefaultProfilePictureUrlString() : profilePictureUrl);

        newUser = userService.createAndSaveUser(newUser);
        
        // Create OAuth mapping
        oauthService.createAndSaveMapping(newUser, externalId, provider);
        
        logger.info("OAuth user registered successfully: " + LoggingUtils.formatUserInfo(newUser));
        return UserMapper.toAuthResponseDTO(newUser);
    }

    @Override
    public AuthResponseDTO handleOAuthRegistrationGracefully(OAuthRegistrationDTO registrationDTO, Exception exception) {
        String email = registrationDTO.getEmail();
        String idToken = registrationDTO.getIdToken();
        OAuthProvider provider = registrationDTO.getProvider();
        
        logger.info("Handling OAuth registration gracefully for exception: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        
        try {
            // First, try to verify the token and get the external ID
            String externalId = null;
            try {
                externalId = oauthService.checkOAuthRegistration(email, idToken, provider);
            } catch (Exception e) {
                logger.warn("Could not verify OAuth token in graceful handler: " + e.getMessage());
                // If we can't verify the token, we can't proceed with any user creation
                return null;
            }
            
            // Check if an existing user can be found and returned
            Optional<AuthResponseDTO> existingUser = oauthService.getUserIfExistsbyExternalId(externalId, email);
            if (existingUser.isPresent()) {
                logger.info("Found existing user in graceful handler, returning user data");
                return existingUser.get();
            }
            
            // If no existing user found, create a new user at EMAIL_VERIFIED status
            // This ensures the user goes through the proper onboarding flow
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(externalId); // Temporary username that will be changed
            newUser.setPhoneNumber(externalId); // Temporary phone that will be changed
            newUser.setStatus(UserStatus.EMAIL_VERIFIED); // Start at EMAIL_VERIFIED for OAuth users
            newUser.setDateCreated(new Date());
            
            String profilePictureUrl = registrationDTO.getProfilePictureUrl();
            newUser.setProfilePictureUrlString(profilePictureUrl == null ? S3Service.getDefaultProfilePictureUrlString() : profilePictureUrl);
            
            String name = registrationDTO.getName();
            if (name != null) {
                newUser.setName(name);
            } else {
                newUser.setName(email.split("@")[0]);
            }
            
            newUser = userService.createAndSaveUser(newUser);
            oauthService.createAndSaveMapping(newUser, externalId, provider);
            
            logger.info("OAuth user created gracefully with EMAIL_VERIFIED status: " + LoggingUtils.formatUserInfo(newUser));
            return UserMapper.toAuthResponseDTO(newUser);
            
        } catch (Exception e) {
            logger.error("Failed to handle OAuth registration gracefully: " + e.getMessage());
            return null;
        }
    }

    @Override
    public HttpHeaders makeHeadersForTokens(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.generateAccessToken(username));
        headers.set("X-Refresh-Token", jwtService.generateRefreshToken(username));
        return headers;
    }

    @Override
    public EmailVerificationResponseDTO sendEmailVerificationCodeForRegistration(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        // Check if user already exists
        if (userService.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        
        EmailVerification verification;
        long secondsUntilNextAttempt;
        
        // Check for existing verification record for this email
        if (emailVerificationRepository.existsByEmail(email)) {
            verification = emailVerificationRepository.findByEmail(email);
            
            // Check if we need to wait before sending another code
            if (verification.getNextSendAttemptAt().isAfter(Instant.now())) {
                long secondsToWait = Duration.between(Instant.now(), verification.getNextSendAttemptAt()).getSeconds();
                return new EmailVerificationResponseDTO(secondsToWait, "Please wait before requesting another verification code");
            }
            
            // Update attempt count and calculate next timeout
            verification.setSendAttempts(verification.getSendAttempts() + 1);
            secondsUntilNextAttempt = getSendVerificationTimeout(verification.getSendAttempts());
            verification.setNextSendAttemptAt(Instant.now().plusSeconds(secondsUntilNextAttempt));
        } else {
            // Create new verification record for registration
            verification = new EmailVerification();
            verification.setEmail(email);
            verification.setSendAttempts(1);
            secondsUntilNextAttempt = getSendVerificationTimeout(verification.getSendAttempts());
            verification.setNextSendAttemptAt(Instant.now().plusSeconds(secondsUntilNextAttempt));
        }

        // Generate verification code and send email
        String verificationCode = VerificationCodeGenerator.generateVerificationCode();
        while (emailVerificationRepository.existsByVerificationCode(passwordEncoder.encode(verificationCode))) {
            verificationCode = VerificationCodeGenerator.generateVerificationCode();
        }

        Instant codeExpiresAt = Instant.now().plusSeconds(600); // 10-minute expiry

        verification.setVerificationCode(passwordEncoder.encode(verificationCode));
        verification.setCodeExpiresAt(codeExpiresAt);

        try {
            String expiryTime = codeExpiresAt.toString();
            emailService.sendVerificationCodeEmail(email, verificationCode, expiryTime);
            logger.info("Email verification code sent for registration to: " + email);
            emailVerificationRepository.save(verification);
            
            return new EmailVerificationResponseDTO(secondsUntilNextAttempt, "Verification code sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send email verification code for registration to: " + email + ": " + e.getMessage());
            throw new RuntimeException("Failed to send verification code", e);
        }
    }

    @Override
    public BaseUserDTO checkEmailVerificationCode(String email, String code) {
        logger.info("Verifying email and creating user for: " + email);

        if (email == null || code == null) {
            throw new IllegalArgumentException("Email and code cannot be null");
        }
        
        // Check if user already exists
        if (userService.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (!emailVerificationRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("No verification record found for this email");
        }
        
        // Find verification record
        EmailVerification verification = emailVerificationRepository.findByEmail(email);
        
        if (verification.getNextCheckAttemptAt() != null && verification.getNextCheckAttemptAt().isAfter(Instant.now())) {
            throw new TooManyAttemptsException("Wait before checking another email verification code: " + verification.getNextCheckAttemptAt().toString());
        }
        
        // Check if code has expired
        if (verification.getCodeExpiresAt().isBefore(Instant.now())) {
            throw new EmailVerificationException("Verification code has expired");
        }
        
        // Check if code matches
        if (!passwordEncoder.matches(code, verification.getVerificationCode())) {
            verification.setCheckAttempts(verification.getCheckAttempts() + 1);
            long secondsToWait = getCheckVerificationTimeout(verification.getCheckAttempts());
            verification.setNextCheckAttemptAt(Instant.now().plusSeconds(secondsToWait));
            emailVerificationRepository.save(verification);
            throw new EmailVerificationException("Incorrect verification code");
        }
        
        // Code is valid, create the user
        User newUser = new User();
        newUser.setId(UUID.randomUUID());
        newUser.setEmail(email);
        newUser.setUsername(email);
        newUser.setName(email);
        newUser.setPhoneNumber(email);
        newUser.setStatus(UserStatus.EMAIL_VERIFIED);
        newUser = userService.createAndSaveUser(newUser);
        
        // Clean up verification record
        emailVerificationRepository.delete(verification);
        
        logger.info("User created successfully after email verification: " + LoggingUtils.formatUserInfo(newUser));
        return UserMapper.toDTO(newUser);
    }

    @Override
    public BaseUserDTO acceptTermsOfService(UUID userId) {
        try {
            logger.info("Accepting Terms of Service for user: " + LoggingUtils.formatUserIdInfo(userId));

            User user = userService.getUserEntityById(userId);
            user.setStatus(UserStatus.ACTIVE);
            user = userService.saveEntity(user);

            logger.info("Successfully updated user status to ACTIVE: " + LoggingUtils.formatUserInfo(user));
            return UserMapper.toDTO(user);
        } catch (Exception e) {
            logger.error("Error accepting Terms of Service for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }


    private long getSendVerificationTimeout(int numAttempts) {
        long secondsToWait;
        if (numAttempts <= 5) {
            secondsToWait = 30;
        } else if (numAttempts <= 10) {
            secondsToWait = 120;
        } else if (numAttempts <= 15) {
            secondsToWait = 600;
        } else {
            secondsToWait = 3600 * 2;
        }
        return secondsToWait;
    }

    private long getCheckVerificationTimeout(int numAttempts) {
        long secondsToWait;
        if (numAttempts <= 5) {
            secondsToWait = 0;
        } else if (numAttempts <= 7) {
            secondsToWait = 30;
        } else if (numAttempts < 10) {
            secondsToWait = 120;
        } else {
            secondsToWait = 3600 * 2; // 2 hours
        }
        return secondsToWait;
    }

    /* ------------------------------ HELPERS ------------------------------ */

    private void checkIfUniqueCredentials(AuthUserDTO authUserDTO) {
        if (userService.existsByEmail(authUserDTO.getEmail())) {
            logger.warn("Registration attempt with existing email: " + authUserDTO.getEmail());
            throw new EmailAlreadyExistsException("Email: " + authUserDTO.getEmail() + " already exists");
        }
        if (userService.existsByUsername(authUserDTO.getUsername())) {
            logger.warn("Registration attempt with existing username: " + authUserDTO.getUsername());
            throw new UsernameAlreadyExistsException("Username: " + authUserDTO.getUsername() + " already exists");
        }
    }

    private UserDTO createAndSaveUser(AuthUserDTO authUserDTO) {
        User user = new User();

        user.setId(UUID.randomUUID()); // can't be null
        user.setUsername(authUserDTO.getUsername());
        user.setEmail(authUserDTO.getEmail());
        user.setPassword(passwordEncoder.encode(authUserDTO.getPassword()));
        user.setName(authUserDTO.getName()); // Set the name from AuthUserDTO
        user.setPhoneNumber(authUserDTO.getUsername()); // Use username as phone number placeholder
        user.setDateCreated(new Date());

        user = userService.createAndSaveUser(user);
        return UserMapper.toDTO(user, List.of(), List.of());
    }

    private void createEmailTokenAndSendEmail(AuthUserDTO authUserDTO) {
        try {
            String emailToken = jwtService.generateEmailToken(authUserDTO.getUsername());
            //String linkToVerification = "http://localhost:8080/api/v1/auth/verify-email?token=" + emailToken; // TODO: change to deployment url
            emailService.sendVerifyAccountEmail(authUserDTO.getEmail(), emailToken);
            //emailService.sendEmail(authUserDTO.getEmail(), "Verify Email", linkToVerification);
        } catch (Exception e) {
            logger.error("Unexpected error while sending email: " + e.getMessage());
        }
    }

}
