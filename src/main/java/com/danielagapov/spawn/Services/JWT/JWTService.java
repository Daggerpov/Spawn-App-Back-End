package com.danielagapov.spawn.Services.JWT;

import com.danielagapov.spawn.Exceptions.Token.BadTokenException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.Token.TokenNotFoundException;
import com.danielagapov.spawn.Services.User.IUserService;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
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
    private static final long EXPIRY = 1000 * 60 * 60 * 24; //  24 hours
    private final ILogger logger;
    private final IUserService userService;


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

    @Override
    public String refreshAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new TokenNotFoundException("No refresh token found");
        }
        // Extract the JWT token from the Authorization header (removing the "Bearer " prefix)
        String token = authHeader.substring(7);
        String username;
        try {
            username = extractUsername(token);
        } catch (Exception e) {
            logger.log("Failed to extract username. Invalid or expired token");
            throw e;
        }
        if (username == null || !userService.existsByUsername(username) ) {
            logger.log("Extracted username does not correspond to any user entity");
            throw new BadTokenException();
        }
        if (isTokenNonExpired(token)) {
            // This is a valid refresh token, grant a new access token to the requester
            String newAccessToken = generateToken(username);
            return newAccessToken;
        } else {
            logger.log("Expired token found");
            throw new BadTokenException();
        }

    }


    /**
     * Helper method used to extract a particular claim from the payload of a JWT.
     * The parameter claimsResolver, is some helper method from a JWT dependency to extract the claim with correct type
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts the entire payload (i.e. all claims) from the JWT which involves parsing the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns whether the token is expired
     */
    private boolean isTokenNonExpired(String token) {
        return !extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * This method generates the signing key for a JWT by converting the base64 encoded SIGNING_SECRET string field
     * of this class into a cryptographic key using HMAC-SHA
     */
    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SIGNING_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
