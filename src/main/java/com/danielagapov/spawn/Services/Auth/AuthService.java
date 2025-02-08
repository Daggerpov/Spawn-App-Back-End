package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.AuthUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
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
    private final IUserRepository repository;

    @Override
    public Map<String, Object> registerUser(AuthUserDTO authUserDTO) {
        try {
            User user = new User();
            user.setId(new UUID(0, 0));
            user.setUsername(authUserDTO.getUsername());
            user.setFirstName("test");
            user.setEmail("blah");
            user.setPassword(passwordEncoder.encode(authUserDTO.getPassword()));
            user = repository.save(user);
            UserDTO userDTO = UserMapper.toDTO(user, null, null);
            //UserDTO userDTO = userService.saveUser(UserMapper.toDTO(user, List.of(), List.of()));

            Map<String, Object> response = new HashMap<>();
            response.put("user", userDTO);
            response.put("token", jwtService.generateToken(user.getUsername()));
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Map<String, Object> loginUser(AuthUserDTO authUserDTO) {
        Map<String, Object> response = new HashMap<>();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authUserDTO.getUsername(),
                        authUserDTO.getPassword()
                )
        );
        if (authentication.isAuthenticated()) {
            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            String token = jwtService.generateToken(username);
            response = new HashMap<>();
            response.put("token", token);
            response.put("user", UserMapper.toDTO(repository.findByUsername(username), List.of(), List.of()));
        }
        return response;
        //return userService.getFullUserByUsername(username);
    }
}
