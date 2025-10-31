package com.danielagapov.spawn.Services.OAuth;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.TokenExpiredException;
import com.danielagapov.spawn.Exceptions.OAuthProviderUnavailableException;
import com.danielagapov.spawn.Util.RetryHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

@Service
public final class AppleOAuthStrategy implements OAuthStrategy {
    private final ILogger logger;

    private final JwkProvider appleJwkProvider;

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${apple.client.id}")
    private String appleClientId;

    @Autowired
    @Lazy
    public AppleOAuthStrategy(ILogger logger) {
        this.logger = logger;

        // Initialize the Apple JWK provider
        this.appleJwkProvider = new JwkProviderBuilder(APPLE_JWKS_URL)
                .cached(10, 24, TimeUnit.HOURS) // Cache up to 10 JWKs for 24 hours
                .rateLimited(10, 1, TimeUnit.MINUTES) // Max 10 requests per minute
                .build();
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.apple;
    }

    @Override
    public String verifyIdToken(String idToken) {
        try {
            logger.info("Verifying Apple ID token");

            if (idToken == null || idToken.isEmpty()) {
                throw new SecurityException("Empty Apple ID token provided");
            }

            // Use retry helper for token verification
            return RetryHelper.executeOAuthWithRetry(() -> {
                try {
                    // Parse the JWT without verifying to get the header and extract the Key ID
                    DecodedJWT decodedJWT = JWT.decode(idToken);

                    // Check token expiration before verification
                    if (decodedJWT.getExpiresAt() != null && decodedJWT.getExpiresAt().before(new java.util.Date())) {
                        logger.error("Apple ID token has expired");
                        throw new TokenExpiredException("Apple ID token has expired, please sign in again");
                    }

                    // Get the kid (Key ID) from the JWT header
                    String keyId = decodedJWT.getKeyId();
                    if (keyId == null) {
                        throw new SecurityException("Key ID not found in Apple ID token header");
                    }

                    // Get the matching JWK from Apple's JWKS endpoint using the Key ID
                    Jwk jwk = appleJwkProvider.get(keyId);

                    // Get the public key from the JWK
                    PublicKey publicKey = jwk.getPublicKey();

                    // Create a verification algorithm using the public key
                    Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) publicKey, null);

                    // Create a verifier for Apple ID tokens
                    JWTVerifier verifier = JWT.require(algorithm)
                            .withIssuer(APPLE_ISSUER)
                            .withAudience(appleClientId)
                            .build();

                    // Verify the token
                    DecodedJWT verifiedJWT = verifier.verify(idToken);

                    // Extract the subject (user ID)
                    String userId = verifiedJWT.getSubject();
                    logger.info("Successfully verified Apple ID token and extracted user ID: " + userId);

                    return userId;

                } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
                    logger.error("Apple ID token has expired: " + e.getMessage());
                    throw new TokenExpiredException("Apple ID token has expired, please sign in again");
                } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
                    logger.error("Apple ID token verification failed: " + e.getMessage());
                    throw new SecurityException("Apple ID token verification failed: " + e.getMessage(), e);
                } catch (com.auth0.jwk.JwkException e) {
                    logger.error("Error retrieving Apple's public key: " + e.getMessage());
                    // Check if it's a network-related error
                    if (e.getCause() instanceof java.net.ConnectException || 
                        e.getCause() instanceof java.net.SocketTimeoutException ||
                        e.getCause() instanceof java.io.IOException) {
                        throw new OAuthProviderUnavailableException("Apple authentication service is temporarily unavailable. Please try again later.", e);
                    } else {
                        throw new SecurityException("Failed to retrieve Apple's public key: " + e.getMessage(), e);
                    }
                }
            });

        } catch (TokenExpiredException e) {
            logger.error("Token expired: " + e.getMessage());
            throw e;
        } catch (OAuthProviderUnavailableException e) {
            logger.error("OAuth provider unavailable: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error verifying Apple ID token: " + e.getMessage());
            throw new SecurityException("Unexpected error verifying Apple ID token: " + e.getMessage(), e);
        }
    }
}
