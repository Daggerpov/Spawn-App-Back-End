package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.AuthUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.IOnboardedUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.FieldAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.Auth.IAuthService;
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
    private final ILogger logger;

    // full path: /api/v1/auth/register
    @PostMapping("register")
    public ResponseEntity<UserDTO> register(@RequestBody() AuthUserDTO authUserDTO) {
        try {
            logger.log(String.format("Account registration request received: {user: %s}", authUserDTO));
            Map<String, Object> response = authService.registerUser(authUserDTO);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + response.get("token"));
            UserDTO newUserDTO = (UserDTO) response.get("user");
            logger.log(String.format("User successfully registered: {user: %s}", newUserDTO));
            return ResponseEntity.ok().headers(headers).body(newUserDTO);
        } catch (FieldAlreadyExistsException fae) {
            logger.log(fae.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception e) {
            logger.log("Error registering in user: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    // full path: /api/v1/auth/login
    @PostMapping("login")
    public ResponseEntity<IOnboardedUserDTO> login(@RequestBody AuthUserDTO authUserDTO) {
        try {
            logger.log(String.format("Login request received: {user: %s}", authUserDTO));
            Map<String, Object> response = authService.loginUser(authUserDTO);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + response.get("token"));
            FullUserDTO existingUserDTO = (FullUserDTO) response.get("user");
            return ResponseEntity.ok().headers(headers).body(existingUserDTO);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            logger.log(String.format("Error logging in user: {user: %s}. Error: %s", authUserDTO, e.getMessage()));
            return ResponseEntity.internalServerError().build();
        }
    }
}
