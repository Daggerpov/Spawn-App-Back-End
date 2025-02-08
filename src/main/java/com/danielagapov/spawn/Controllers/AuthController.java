package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.AuthUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.Auth.IAuthService;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final IAuthService authService;

    // full path: /api/v1/auth/register
    @PostMapping("register")
    public ResponseEntity<UserDTO> register(@RequestBody AuthUserDTO authUserDTO) {
        try {
            Map<String, Object> response = authService.registerUser(authUserDTO);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + response.get("token"));
            UserDTO newUserDTO = (UserDTO) response.get("user");
            return ResponseEntity.ok().headers(headers).body(newUserDTO);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    // full path: /api/v1/auth/login
    @PostMapping("login")
    public ResponseEntity<UserDTO> login(@RequestBody AuthUserDTO authUserDTO) {
        try {
            Map<String, Object> response = authService.loginUser(authUserDTO);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + response.get("token"));
            UserDTO existingUserDTO = (UserDTO) response.get("user");
            return ResponseEntity.ok().headers(headers).body(existingUserDTO);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
