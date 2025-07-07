package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Exceptions.*;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.PhoneNumberVerification;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IPhoneNumberVerificationRepository;
import com.danielagapov.spawn.Services.Email.IEmailService;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import com.danielagapov.spawn.Services.SMS.ISMSVerificationService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.LoggingUtils;
import com.danielagapov.spawn.Util.PhoneNumberValidator;
import lombok.AllArgsConstructor;
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
    private final ISMSVerificationService verificationService;
    private final IOAuthService oauthService;
    private final IPhoneNumberVerificationRepository verificationRepository;


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
    public void sendVerificationCode(String phoneNumber, UUID userId) {
        phoneNumber = PhoneNumberValidator.cleanPhoneNumber(phoneNumber);
        if (!PhoneNumberValidator.isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Bad phone number");
        }
        if (userService.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyExistsException("Phone number already exists");
        }
        if (!userService.existsByUserId(userId)) {
            throw new AccountNotFoundException("Account not found");
        }
        User user = userService.getUserEntityById(userId);
        if (user.getStatus() == UserStatus.PHONE_NUMBER_VERIFIED || user.getStatus() == UserStatus.ACTIVE) {
            logger.info("User is already verified, returning user");
            return;
        }

        user.setPhoneNumber(phoneNumber + " (temp)");
        userService.saveEntity(user);
        if (verificationRepository.existsByUserId(userId)) {
            logger.info("Found existing verification record for user with ID: " + userId);
            PhoneNumberVerification verification = verificationRepository.findByUserId(userId);

            if (verification.getNextSendAttemptAt().isBefore(Instant.now())) {
                verification.getAttemptedPhoneNumbers().add(phoneNumber);
                verification.setSendAttempts(verification.getSendAttempts() + 1);
                long secondsToWait = getSendVerificationTimeout(verification.getSendAttempts());

                verification.setNextSendAttemptAt(Instant.now().plusSeconds(secondsToWait));
                verificationRepository.save(verification);
            } else {
                throw new TooManyAttemptsException("Wait before sending another SMS verification code: " + verification.getNextSendAttemptAt().toString() + " (in seconds)");
            }
        } else {
            logger.info("No existing verification record for user with ID: " + userId + ". Creating one now");
            PhoneNumberVerification verification = new PhoneNumberVerification();
            verification.setUser(user);
            verification.getAttemptedPhoneNumbers().add(phoneNumber);
            verification.setSendAttempts(1);
            verification.setNextSendAttemptAt(Instant.now().plusSeconds(30));
            verificationRepository.save(verification);

        }
        verificationService.sendSMSVerification(phoneNumber);
    }

    @Override
    public BaseUserDTO checkVerificationCode(String phoneNumber, String code, UUID userId) {
        logger.info("Checking verification code for phone number");
        User user;
        if (!userService.existsByUserId(userId)) {
            throw new AccountNotFoundException("Account not found");
        }
        user = userService.getUserEntityById(userId);
        if (user.getStatus() == UserStatus.PHONE_NUMBER_VERIFIED || user.getStatus() == UserStatus.ACTIVE) {
            logger.info("User is already verified, returning user");
            return UserMapper.toDTO(user);
        }
        if (!user.getPhoneNumber().contains(phoneNumber)) {
            throw new AccountNotFoundException("Given phone number does not match user's phone number");
        }
        if (!verificationRepository.existsByUserId(userId)) {
            // A user should ALWAYS have a verification record at this point, since they would have requested a verification code first
            throw new IllegalArgumentException("User does not have a verification record");
        }
        PhoneNumberVerification verification = verificationRepository.findByUserId(userId);
        if (!verification.getAttemptedPhoneNumbers().contains(phoneNumber)) {
            throw new IllegalArgumentException("Phone number has not received verification code");
        }
        if (verification.getNextCheckAttemptAt().isAfter(Instant.now())) {
            throw new TooManyAttemptsException("Wait before checking another SMS verification code: " + verification.getNextCheckAttemptAt().toString());
        }

        if (verificationService.checkSMSVerification(phoneNumber, code)) {
            user.setPhoneNumber(phoneNumber);
            user.setStatus(UserStatus.PHONE_NUMBER_VERIFIED);
            verificationRepository.delete(verification); // No longer needed
            return UserMapper.toDTO(userService.saveEntity(user));
        } else {
            verification.setCheckAttempts(verification.getCheckAttempts() + 1);
            long secondsToWait = getCheckVerificationTimeout(verification.getCheckAttempts());
            verification.setNextCheckAttemptAt(Instant.now().plusSeconds(secondsToWait));
            verificationRepository.save(verification);
            throw new SMSVerificationException("Incorrect verification code");
        }
    }

    @Override
    public BaseUserDTO registration(String email, String externalIdToken, OAuthProvider provider) {
        if (email == null && externalIdToken == null) {
            throw new IllegalArgumentException("Email and externalId cannot both be null");
        }
        User newUser;
        if (externalIdToken != null) { // OAuth registration
            String externalId = oauthService.checkOAuthRegistration(email, externalIdToken, provider);
            newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(externalId);
            newUser.setPhoneNumber(externalId);
            newUser.setStatus(UserStatus.EMAIL_REGISTERED);
            newUser = userService.saveEntity(newUser);
            oauthService.createAndSaveMapping(newUser, externalId, provider);
        } else { // Email registration
            if (userService.existsByEmail(email)) {
                throw new EmailAlreadyExistsException("Email already exists");
            }
            newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email);
            newUser.setPhoneNumber(email);
            newUser.setStatus(UserStatus.EMAIL_REGISTERED);
            newUser = userService.saveEntity(newUser);
        }
        return UserMapper.toDTO(newUser);
    }

    private long getSendVerificationTimeout(int numAttempts) {
        long secondsToWait;
        if (numAttempts < 3) {
            secondsToWait = 30;
        } else if (numAttempts < 8) {
            secondsToWait = 120;
        } else {
            secondsToWait = 600;
        }
        return secondsToWait;
    }

    private long getCheckVerificationTimeout(int numAttempts) {
        long secondsToWait;
        if (numAttempts < 3) {
            secondsToWait = 0;
        } else if (numAttempts < 6) {
            secondsToWait = 30;
        } else if (numAttempts < 8) {
            secondsToWait = 120;
        } else {
            secondsToWait = 600;
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
