package com.danielagapov.spawn.Services.JWT;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@AllArgsConstructor
public class JWTService implements IJWTService {
    private static final String SIGNING_SECRET;
    static {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            SIGNING_SECRET = dotenv.get("SIGNING_KEY");

    }
    private static final long EXPIRY = 1000 * 60 * 4; // 4 mins
    private final ILogger logger;


    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isValidToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && isTokenNonExpired(token);
    }

    @Override
    public String generateToken(String username) {
        logger.log("Generating JWT token for user: " + username);
        Map<String, Object> claims = new HashMap<>();
        try {
            return Jwts.builder()
                    .claims()
                    .add(claims)
                    .subject(username)
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + EXPIRY))
                    .and()
                    .signWith(getKey())
                    .compact()
                    ;
        } catch (Exception e) {
            logger.log("Error generating JWT token: " + e.getMessage());
            throw e;
        }
    }


    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenNonExpired(String token) {
        return !extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SIGNING_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
