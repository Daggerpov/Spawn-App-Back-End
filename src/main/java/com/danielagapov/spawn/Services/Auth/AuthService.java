package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Exceptions.EmailAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.FieldAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.UsernameAlreadyExistsException;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Services.Email.IEmailService;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.LoggingUtils;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    @Override
    public BaseUserDTO registerUser(AuthUserDTO authUserDTO) throws FieldAlreadyExistsException {
        logger.info("Attempting to register new user with username: " + authUserDTO.getUsername());
        checkIfUniqueCredentials(authUserDTO);
        try {
            BaseUserDTO userDTO = createAndSaveUser(authUserDTO);
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

    private BaseUserDTO createAndSaveUser(AuthUserDTO authUserDTO) {
        User user = new User();

        user.setId(UUID.randomUUID()); // can't be null
        user.setUsername(authUserDTO.getUsername());
        user.setEmail(authUserDTO.getEmail());
        user.setPassword(passwordEncoder.encode(authUserDTO.getPassword()));
        user.setVerified(false);
        user.setDateCreated(new Date());

        user = userService.createAndSaveUser(user);
        return UserMapper.toDTO(user);
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
