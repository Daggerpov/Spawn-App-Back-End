package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.AuthUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.IOnboardedUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.EmailAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.FieldAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.UsernameAlreadyExistsException;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.User.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService implements IAuthService {
    private final IJWTService jwtService;
    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ILogger logger;

    @Override
    public Map<String, Object> registerUser(AuthUserDTO authUserDTO) throws FieldAlreadyExistsException {
        checkIfUniqueCredentials(authUserDTO);
        try {
            UserDTO userDTO = createAndSaveUser(authUserDTO);
            String token = jwtService.generateToken(userDTO.username());
            return makeMap(token, userDTO);
        } catch (Exception e) {
            logger.log("Unexpected error while registering user");
            throw e;
        }
    }

    @Override
    public Map<String, Object> loginUser(AuthUserDTO authUserDTO) {
        Map<String, Object> response = new HashMap<>();
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
            String token = jwtService.generateToken(username);
            logger.log("Fetching full user dto");
            FullUserDTO fullUserDTO = userService.getFullUserByUsername(username);
            return makeMap(token, fullUserDTO);
        }
        return response;
    }

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

        user = userService.saveEntity(user);
        return UserMapper.toDTO(user, List.of(), List.of());
    }

    private Map<String, Object> makeMap(String token, IOnboardedUserDTO userDTO) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userDTO);
        return response;
    }
}
