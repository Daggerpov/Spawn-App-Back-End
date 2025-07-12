package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Exceptions.*;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.EmailVerification;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IEmailVerificationRepository;
import com.danielagapov.spawn.Services.Email.IEmailService;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
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

import java.time.Instant;
import java.util.Date;
import java.util.List;
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
    public BaseUserDTO loginUser(AuthUserDTO authUserDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authUserDTO.getUsername(),
                        authUserDTO.getPassword()
                )
        );
        if (authentication.isAuthenticated()) {
            String username = ((UserDetails) authentication.getPrincipal()).getUsername();

            User user = userService.getUserEntityByUsername(username);
            return UserMapper.toDTO(user);
        } else {
            logger.warn("Failed authentication attempt for username: " + authUserDTO.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Override
    public boolean verifyEmail(String token) {
        try {
            if (jwtService.isValidEmailToken(token)) {
                // The email token is valid so mark this user as verified user in database
                final String username = jwtService.extractUsername(token);
                logger.info("Verifying email for user with username: " + username);
                
                User user = userService.getUserEntityByUsername(username);
                user.setVerified(true);
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
    public BaseUserDTO getUserByToken(String token) {
        final String username = jwtService.extractUsername(token);
        return userService.getBaseUserByUsername(username);
    }


    @Override
    public BaseUserDTO registerUserViaOAuth(String email, String externalIdToken, OAuthProvider provider) {
        if (email == null || externalIdToken == null) {
            throw new IllegalArgumentException("Email and externalIdToken cannot be null for OAuth registration");
        }
        
        // Check if user already exists
        if (userService.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        
        // Verify OAuth token and get external ID
        String externalId = oauthService.checkOAuthRegistration(email, externalIdToken, provider);
        
        // Create verified user immediately for OAuth
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(externalId);
        newUser.setPhoneNumber(externalId);
        newUser.setStatus(UserStatus.EMAIL_VERIFIED); // OAuth users are automatically verified
        newUser.setDateCreated(new Date());
        newUser = userService.saveEntity(newUser);
        
        // Create OAuth mapping
        oauthService.createAndSaveMapping(newUser, externalId, provider);
        
        logger.info("OAuth user registered successfully: " + LoggingUtils.formatUserInfo(newUser));
        return UserMapper.toDTO(newUser);
    }

    @Override
    public HttpHeaders makeHeadersForTokens(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtService.generateAccessToken(username));
        headers.set("X-Refresh-Token", jwtService.generateRefreshToken(username));
        return headers;
    }

    @Override
    public void sendEmailVerificationCodeForRegistration(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        // Check if user already exists
        if (userService.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        
        // Check for existing verification record for this email
        if (emailVerificationRepository.existsByEmail(email)) {
            EmailVerification verification = emailVerificationRepository.findByEmail(email);
            
            if (verification != null && verification.getNextSendAttemptAt().isAfter(Instant.now())) {
                verification.setSendAttempts(verification.getSendAttempts() + 1);
                long secondsToWait = getSendVerificationTimeout(verification.getSendAttempts());
                verification.setNextSendAttemptAt(Instant.now().plusSeconds(secondsToWait));
                emailVerificationRepository.save(verification);
            } else if (verification != null) {
                throw new TooManyAttemptsException("Wait before sending another email verification code: " + verification.getNextSendAttemptAt().toString() + " (in seconds)");
            } else {
                throw new RuntimeException("Unexpected error while checking for existing verification record for email: " + email);
            }
        } else {
            // Create new verification record for registration
            EmailVerification verification = new EmailVerification();
            verification.setEmail(email);
            verification.setSendAttempts(1);
            verification.setNextSendAttemptAt(Instant.now().plusSeconds(30));
            emailVerificationRepository.save(verification);
        }

        EmailVerification verification;
        if (emailVerificationRepository.existsByEmail(email)) {
            verification = emailVerificationRepository.findByEmail(email);

            if (verification.getNextSendAttemptAt().isAfter(Instant.now())) {
                verification.setSendAttempts(verification.getSendAttempts() + 1);
                long secondsToWait = getSendVerificationTimeout(verification.getSendAttempts());
                verification.setNextSendAttemptAt(Instant.now().plusSeconds(secondsToWait));
            } else {
                throw new TooManyAttemptsException("Wait before sending another email verification code: " + verification.getNextSendAttemptAt().toString() + " (in seconds)");
            }
        } else {
            verification = new EmailVerification();
            verification.setEmail(email);
            verification.setSendAttempts(1);
            long secondsToWait = getSendVerificationTimeout(verification.getSendAttempts());
            verification.setNextSendAttemptAt(Instant.now().plusSeconds(secondsToWait));
        }

        // Generate verification code and send email
        String verificationCode = VerificationCodeGenerator.generateVerificationCode();
        while (!emailVerificationRepository.existsByVerificationCode(verificationCode)) {
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
        newUser.setStatus(UserStatus.EMAIL_VERIFIED);
        newUser = userService.saveEntity(newUser);
        
        // Clean up verification record
        emailVerificationRepository.delete(verification);
        
        logger.info("User created successfully after email verification: " + LoggingUtils.formatUserInfo(newUser));
        return UserMapper.toDTO(newUser);
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
        user.setVerified(false);
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
