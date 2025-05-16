package com.danielagapov.spawn.Services.JWT;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.Token.BadTokenException;
import com.danielagapov.spawn.Exceptions.Token.TokenNotFoundException;
import com.danielagapov.spawn.Services.User.IUserService;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
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
// TODO: consider refactor to type hierarchy with AccessToken, RefreshToken, EmailToken extending JWTService
public class JWTService implements IJWTService {
    private static final String SIGNING_SECRET;

    private enum TokenType {ACCESS, REFRESH, EMAIL}

    static {
        final String secret = System.getenv("SIGNING_SECRET");
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        SIGNING_SECRET = secret == null ? dotenv.get("SIGNING_SECRET") : secret;
    }

    private static final long ACCESS_TOKEN_EXPIRY = 1000L * 60 * 60 * 24; //  24 hours
    private static final long REFRESH_TOKEN_EXPIRY = 1000L * 60 * 60 * 24 * 180; // 180 days or 6 months
    private static final long EMAIL_TOKEN_EXPIRY = 1000L * 60 * 60 * 24; // 24 hours
    private final ILogger logger;
    private final IUserService userService;


    @Override
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (ExpiredJwtException e) {
            logger.warn("Token has expired: " + e.getMessage());
            throw e;
        } catch (SignatureException e) {
            logger.warn("Invalid token signature: " + e.getMessage());
            throw e;
        } catch (JwtException e) {
            logger.warn("JWT parsing error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error extracting username: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isValidToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && isTokenNonExpired(token) && isMatchingTokenType(token, TokenType.ACCESS);
        } catch (Exception e) {
            logger.warn("Token validation failed: " + e.getMessage());
            return false;
        }
    }


    @Override
    public String generateAccessToken(String username) {
        logger.info("Generating access token for user: " + username);
        Map<String, Object> claims = makeClaims(TokenType.ACCESS);
        return generateToken(username, ACCESS_TOKEN_EXPIRY, claims);
    }

    @Override
    public String refreshAccessToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new TokenNotFoundException("No refresh token found");
        }
        // Extract the JWT token from the Authorization header (removing the "Bearer " prefix)
        final String token = authHeader.substring(7);
        final String username;
        try {
            username = extractUsername(token);
        } catch (Exception e) {
            logger.error("Failed to extract username. Invalid or expired token");
            throw e;
        }
        if (username == null || !userService.existsByUsername(username)) {
            logger.warn("Extracted username does not correspond to any user entity");
            throw new BadTokenException();
        }
        if (isTokenNonExpired(token) && isMatchingTokenType(token, TokenType.REFRESH)) {
            // This is a valid refresh token, grant a new access token to the requester
            String newAccessToken = generateAccessToken(username);
            return newAccessToken;
        } else {
            logger.warn("Expired token found");
            throw new BadTokenException();
        }
    }

    @Override
    public String generateRefreshToken(String username) {
        logger.info("Generating refresh token for user: " + username);
        Map<String, Object> claims = makeClaims(TokenType.REFRESH);
        return generateToken(username, REFRESH_TOKEN_EXPIRY, claims);
    }

    @Override
    public String generateEmailToken(String username) {
        logger.info("Generating email token for user: " + username);
        Map<String, Object> claims = makeClaims(TokenType.EMAIL);
        return generateToken(username, EMAIL_TOKEN_EXPIRY, claims);
    }

    @Override
    public boolean isValidEmailToken(String token) {
        try {
            return isTokenNonExpired(token) && isMatchingTokenType(token, TokenType.EMAIL);
        } catch (Exception e) {
            logger.warn("Email token validation failed: " + e.getMessage());
            return false;
        }
    }


    /* ------------------------------ HELPERS ------------------------------ */

    private String generateToken(String username, long expiry, Map<String, Object> claims) {
        try {
            return Jwts.builder()
                    .claims()
                    .add(claims)
                    .subject(username)
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + expiry))
                    .and()
                    .signWith(getKey())
                    .compact();
        } catch (Exception e) {
            logger.error("Error generating JWT token: " + e.getMessage());
            throw e;
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
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.warn("JWT parsing error in extractAllClaims: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in extractAllClaims: " + e.getMessage());
            throw e;
        }
    }

    private TokenType extractTokenType(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String typeAsString = (String) claims.get("type");
            return TokenType.valueOf(typeAsString); // returns "type" claim as TokenType
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid token type value: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.warn("Error extracting token type: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Returns whether the token is expired
     */
    private boolean isTokenNonExpired(String token) {
        try {
            return !extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException e) {
            logger.warn("Token has expired");
            return false;
        } catch (Exception e) {
            logger.warn("Error checking token expiration: " + e.getMessage());
            throw e;
        }
    }

    /**
     * This method generates the signing key for a JWT by converting the base64 encoded secret string field
     * into a cryptographic key using HMAC-SHA
     */
    private SecretKey getKey() {
        try {
            final byte[] keyBytes = Decoders.BASE64.decode(SIGNING_SECRET);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            logger.error("Error generating signing key: " + e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> makeClaims(TokenType type) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("type", type);
        return claims;
    }

    private boolean isMatchingTokenType(String token, TokenType tokenType) {
        try {
            final TokenType type = extractTokenType(token);
            return type == tokenType;
        } catch (Exception e) {
            logger.warn("Error matching token type: " + e.getMessage());
            return false;
        }
    }
}
