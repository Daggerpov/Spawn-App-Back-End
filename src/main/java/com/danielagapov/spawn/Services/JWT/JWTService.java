package com.danielagapov.spawn.Services.JWT;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JWTService implements IJWTService {
    private static final String SIGNING_SECRET = System.getenv("SIGNING_KEY");

    @Override
    public String extractUsername(String token) {
        return "";
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 3))
                .and()
                .signWith(getKey())
                .compact()
                ;
    }

    private Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SIGNING_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
