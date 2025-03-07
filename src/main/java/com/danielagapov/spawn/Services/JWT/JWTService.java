package com.danielagapov.spawn.Services.JWT;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.Token.BadTokenException;
import com.danielagapov.spawn.Exceptions.Token.TokenNotFoundException;
import com.danielagapov.spawn.Services.User.IUserService;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isValidToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && isTokenNonExpired(token) && isMatchingTokenType(token, TokenType.ACCESS);
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
        return isTokenNonExpired(token) && isMatchingTokenType(token, TokenType.EMAIL);
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
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private TokenType extractTokenType(String token) {
        Claims claims = extractAllClaims(token);
        String typeAsString = (String) claims.get("type");
        return TokenType.valueOf(typeAsString); // returns "type" claim as TokenType

    }

    /**
     * Returns whether the token is expired
     */
    private boolean isTokenNonExpired(String token) {
        return !extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * This method generates the signing key for a JWT by converting the base64 encoded secret string field
     * into a cryptographic key using HMAC-SHA
     */
    private SecretKey getKey() {
        final byte[] keyBytes = Decoders.BASE64.decode(SIGNING_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Map<String, Object> makeClaims(TokenType type) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("type", type);
        return claims;
    }

    private boolean isMatchingTokenType(String token, TokenType tokenType) {
        final TokenType type = extractTokenType(token);
        return type == tokenType;

    }
}
