package com.danielagapov.spawn.Services.JWT;

import org.springframework.stereotype.Service;

@Service
public class JWTService implements IJWTService {

    @Override
    public String extractUsername(String token) {
        return "";
    }
}
