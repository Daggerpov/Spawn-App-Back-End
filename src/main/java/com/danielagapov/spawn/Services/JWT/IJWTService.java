package com.danielagapov.spawn.Services.JWT;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;

public interface IJWTService {
    /**
     * A JWT has the following structure:
     * - Header: contains info about token type and signing algorithm
     * - Payload: contains data (or claims) about the user and token such as subject and issued and expiration time
     * - Signature: composed of the header, payload, and a secret that was specified at token creation, and is used to
     *   verify the integrity of the token
     */

    /**
     * Extracts the username (from 'subject' claim) from the 'payload' of the JWT. This username is used for setting authentication
     * for incoming requests.
     */
    String extractUsername(String token);

    /**
     * Determines whether the JWT is valid by checking for expiry,
     */
    boolean isValidToken(String token, UserDetails userDetails);

    /**
     * Generates a JWT with the given username as the subject claim
     */
    String generateAccessToken(String username);

    String refreshAccessToken(HttpServletRequest request);

    String generateRefreshToken(String username);

    /**
     * Generates a JWT for email verification
     */
    String generateEmailToken(String username);

    boolean isValidEmailToken(String token);
}
