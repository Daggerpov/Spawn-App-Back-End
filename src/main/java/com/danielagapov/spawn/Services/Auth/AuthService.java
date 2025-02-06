package com.danielagapov.spawn.Services.Auth;

import com.danielagapov.spawn.DTOs.AuthUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
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
import java.util.Map;

@Service
@AllArgsConstructor
public class AuthService implements IAuthService {
    private final IJWTService jwtService;
    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public Map<String, Object> registerUser(AuthUserDTO authUserDTO) {
        User user = new User();
        user.setUsername(authUserDTO.getUsername());
        user.setFirstName("test");
        user.setEmail("blah");
        user.setPassword(passwordEncoder.encode(authUserDTO.getPassword()));
        UserDTO userDTO = userService.saveUser(UserMapper.toDTO(user, null, null));

        Map<String, Object> response = new HashMap<>();
        response.put("user", userDTO);

        jwtService.generateToken(user.getUsername());

        response.put("token", jwtService.generateToken(user.getUsername()));
        return response;
    }

    @Override
    public FullUserDTO loginUser(AuthUserDTO authUserDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authUserDTO.getUsername(),
                        authUserDTO.getPassword()
                )
        );
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userService.getFullUserByUsername(username);
    }
}
