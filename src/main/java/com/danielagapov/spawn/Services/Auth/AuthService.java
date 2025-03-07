package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.DTOs.User.FullUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Exceptions.EmailAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.FieldAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.UsernameAlreadyExistsException;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.Email.IEmailService;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.User.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Override
    public UserDTO registerUser(AuthUserDTO authUserDTO) throws FieldAlreadyExistsException {
        checkIfUniqueCredentials(authUserDTO);
        try {
            UserDTO userDTO = createAndSaveUser(authUserDTO);
            createEmailTokenAndSendEmail(authUserDTO);
            return userDTO;
        } catch (Exception e) {
            logger.log("Unexpected error while registering user");
            throw e;
        }
    }

    @Override
    public FullUserDTO loginUser(AuthUserDTO authUserDTO) {
        logger.log(String.format("Attempting to login user: { user: %s }", authUserDTO));
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authUserDTO.getUsername(),
                        authUserDTO.getPassword()
                )
        );
        if (authentication.isAuthenticated()) {
            logger.log("Authentication successful for user: " + authUserDTO.getUsername());
            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            logger.log("Fetching full user dto");
            FullUserDTO fullUserDTO = userService.getFullUserByUsername(username);
            return fullUserDTO;
        } else {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Override
    public boolean verifyEmail(String token) {
        if (jwtService.isValidEmailToken(token)) {
            // The email token is valid so mark this user as verified user in database
            final String username = jwtService.extractUsername(token);
            userService.verifyUserByUsername(username);
            return true;
        }
        // TODO: consider deleting user here
        return false;
    }

    /* ------------------------------ HELPERS ------------------------------ */

    private void checkIfUniqueCredentials(AuthUserDTO authUserDTO) {
        if (userService.existsByEmail(authUserDTO.getEmail())) {
            throw new EmailAlreadyExistsException("Email: " + authUserDTO.getEmail() + " already exists");
        }
        if (userService.existsByUsername(authUserDTO.getUsername())) {
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

        user = userService.saveEntity(user);
        return UserMapper.toDTO(user, List.of(), List.of());
    }

    private void createEmailTokenAndSendEmail(AuthUserDTO authUserDTO) {
        try {
            String emailToken = jwtService.generateEmailToken(authUserDTO.getUsername());
            //String linkToVerification = "http://localhost:8080/api/v1/auth/verify-email?token=" + emailToken; // TODO: change to deployment url
            emailService.sendVerifyAccountEmail(authUserDTO.getEmail(), emailToken);
            //emailService.sendEmail(authUserDTO.getEmail(), "Verify Email", linkToVerification);
        } catch (Exception e) {
            logger.log("Unexpected error while sending email: " + e.getMessage());
        }
    }

}
