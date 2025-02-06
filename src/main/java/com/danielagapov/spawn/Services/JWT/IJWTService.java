package com.danielagapov.spawn.Services.JWT;

import org.springframework.security.core.userdetails.UserDetails;

public interface IJWTService {
    String extractUsername(String token);
    boolean isValidToken(String token, UserDetails userDetails);
    String generateToken(String username);
}
