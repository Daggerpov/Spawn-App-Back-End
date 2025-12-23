package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.auth.internal.services.IJWTService;
import com.danielagapov.spawn.auth.internal.services.JWTService;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.exceptions.Token.BadTokenException;
import com.danielagapov.spawn.shared.exceptions.Token.TokenNotFoundException;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.services.IUserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JWTService - Critical security component.
 * Tests token generation, validation, and security features.
 * 
 * Note: These tests require the SIGNING_SECRET environment variable to be set.
 * If not set, the tests will be skipped.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Order(1)
class JWTServiceTests {

    @Mock
    private ILogger logger;

    @Mock
    private IUserService userService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private HttpServletRequest httpServletRequest;

    private IJWTService jwtService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_SIGNING_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGVzdGluZy10aGF0LWlzLWxvbmctZW5vdWdo";
    
    private static boolean signingSecretSet = false;

    @BeforeAll
    void setupAll() throws Exception {
        // Try to set the SIGNING_SECRET using reflection if not already set
        String existingSecret = System.getenv("SIGNING_SECRET");
        if (existingSecret == null || existingSecret.isEmpty()) {
            // Use reflection to set the static SIGNING_SECRET field
            try {
                setSigningSecret(TEST_SIGNING_SECRET);
                signingSecretSet = true;
            } catch (Exception e) {
                System.err.println("Could not set SIGNING_SECRET via reflection: " + e.getMessage());
                signingSecretSet = false;
            }
        } else {
            signingSecretSet = true;
        }
    }
    
    private void setSigningSecret(String secret) throws Exception {
        Field signingSecretField = JWTService.class.getDeclaredField("SIGNING_SECRET");
        signingSecretField.setAccessible(true);
        
        // Remove final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(signingSecretField, signingSecretField.getModifiers() & ~Modifier.FINAL);
        
        signingSecretField.set(null, secret);
    }

    @BeforeEach
    void setup() {
        // Skip tests if signing secret is not available
        assumeTrue(signingSecretSet, "SIGNING_SECRET not configured - skipping JWT tests");
        
        // Reset mocks and create fresh service instance
        lenient().doNothing().when(logger).info(anyString());
        lenient().doNothing().when(logger).warn(anyString());
        lenient().doNothing().when(logger).error(anyString());
        
        jwtService = new JWTService(logger, userService);
    }

    @Nested
    @DisplayName("Access Token Generation Tests")
    class AccessTokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT access token for username")
        void shouldGenerateValidAccessToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // When
            String token = jwtService.generateAccessToken(TEST_USERNAME);

            // Then
            assertThat(token)
                .isNotNull()
                .isNotEmpty()
                .contains(".");  // JWT format: header.payload.signature
            
            // JWT should have 3 parts separated by dots
            String[] parts = token.split("\\.");
            assertThat(parts).hasSize(3);
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // When
            String token1 = jwtService.generateAccessToken("user1");
            String token2 = jwtService.generateAccessToken("user2");

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should generate different tokens for same user at different times")
        void shouldGenerateDifferentTokensAtDifferentTimes() throws InterruptedException {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // When
            String token1 = jwtService.generateAccessToken(TEST_USERNAME);
            Thread.sleep(10); // Small delay to ensure different issuedAt
            String token2 = jwtService.generateAccessToken(TEST_USERNAME);

            // Then - Tokens should be different due to different issuedAt timestamps
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should log token generation")
        void shouldLogTokenGeneration() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // When
            jwtService.generateAccessToken(TEST_USERNAME);

            // Then
            verify(logger).info(contains("Generating access token"));
        }
    }

    @Nested
    @DisplayName("Refresh Token Generation Tests")
    class RefreshTokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT refresh token")
        void shouldGenerateValidRefreshToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // When
            String token = jwtService.generateRefreshToken(TEST_USERNAME);

            // Then
            assertThat(token)
                .isNotNull()
                .isNotEmpty()
                .contains(".");
        }

        @Test
        @DisplayName("Should generate different refresh token than access token")
        void shouldGenerateDifferentRefreshToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // When
            String accessToken = jwtService.generateAccessToken(TEST_USERNAME);
            String refreshToken = jwtService.generateRefreshToken(TEST_USERNAME);

            // Then - Tokens should be different (different type claims)
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

    @Nested
    @DisplayName("Email Token Generation Tests")
    class EmailTokenGenerationTests {

        @Test
        @DisplayName("Should generate valid email verification token")
        void shouldGenerateValidEmailToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // When
            String token = jwtService.generateEmailToken(TEST_USERNAME);

            // Then
            assertThat(token)
                .isNotNull()
                .isNotEmpty()
                .contains(".");
        }

        @Test
        @DisplayName("Should validate email token")
        void shouldValidateEmailToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String token = jwtService.generateEmailToken(TEST_USERNAME);

            // When
            boolean isValid = jwtService.isValidEmailToken(token);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject access token as email token")
        void shouldRejectAccessTokenAsEmailToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String accessToken = jwtService.generateAccessToken(TEST_USERNAME);

            // When
            boolean isValid = jwtService.isValidEmailToken(accessToken);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Username Extraction Tests")
    class UsernameExtractionTests {

        @Test
        @DisplayName("Should extract username from access token")
        void shouldExtractUsernameFromAccessToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String token = jwtService.generateAccessToken(TEST_USERNAME);

            // When
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("Should extract username from refresh token")
        void shouldExtractUsernameFromRefreshToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String token = jwtService.generateRefreshToken(TEST_USERNAME);

            // When
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("Should extract email from email token")
        void shouldExtractEmailFromEmailToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String token = jwtService.generateEmailToken(TEST_EMAIL);

            // When
            String extractedEmail = jwtService.extractUsername(token);

            // Then
            assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate token with matching username")
        void shouldValidateTokenWithMatchingUsername() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String token = jwtService.generateAccessToken(TEST_USERNAME);
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);

            // When
            boolean isValid = jwtService.isValidToken(token, userDetails);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token with non-matching username")
        void shouldRejectTokenWithNonMatchingUsername() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String token = jwtService.generateAccessToken(TEST_USERNAME);
            when(userDetails.getUsername()).thenReturn("differentuser");

            // When
            boolean isValid = jwtService.isValidToken(token, userDetails);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject refresh token when validating as access token")
        void shouldRejectRefreshTokenAsAccessToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String refreshToken = jwtService.generateRefreshToken(TEST_USERNAME);
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);

            // When
            boolean isValid = jwtService.isValidToken(refreshToken, userDetails);

            // Then - Should be false because token type doesn't match
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should throw TokenNotFoundException when no auth header")
        void shouldThrowWhenNoAuthHeader() {
            // Given
            when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> jwtService.refreshAccessToken(httpServletRequest))
                .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw TokenNotFoundException when auth header missing Bearer prefix")
        void shouldThrowWhenMissingBearerPrefix() {
            // Given
            when(httpServletRequest.getHeader("Authorization")).thenReturn("invalid-header");

            // When/Then
            assertThatThrownBy(() -> jwtService.refreshAccessToken(httpServletRequest))
                .isInstanceOf(TokenNotFoundException.class);
        }

        @Test
        @DisplayName("Should refresh access token with valid refresh token")
        void shouldRefreshAccessTokenWithValidRefreshToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String refreshToken = jwtService.generateRefreshToken(TEST_USERNAME);
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + refreshToken);
            when(userService.existsByUsername(TEST_USERNAME)).thenReturn(true);

            // When
            String newAccessToken = jwtService.refreshAccessToken(httpServletRequest);

            // Then
            assertThat(newAccessToken)
                .isNotNull()
                .isNotEmpty();
            
            // Verify the new token is for the same user
            String extractedUsername = jwtService.extractUsername(newAccessToken);
            assertThat(extractedUsername).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("Should handle OAuth user with email as subject")
        void shouldHandleOAuthUserWithEmailAsSubject() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String refreshToken = jwtService.generateRefreshToken(TEST_EMAIL);
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + refreshToken);
            when(userService.existsByUsername(TEST_EMAIL)).thenReturn(false);
            when(userService.existsByEmail(TEST_EMAIL)).thenReturn(true);
            
            User mockUser = new User();
            mockUser.setEmail(TEST_EMAIL);
            mockUser.setUsername(null); // OAuth user without username
            when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(mockUser);

            // When
            String newAccessToken = jwtService.refreshAccessToken(httpServletRequest);

            // Then
            assertThat(newAccessToken).isNotNull();
        }

        @Test
        @DisplayName("Should throw BadTokenException for non-existent user")
        void shouldThrowForNonExistentUser() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String refreshToken = jwtService.generateRefreshToken("nonexistent");
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + refreshToken);
            when(userService.existsByUsername("nonexistent")).thenReturn(false);
            when(userService.existsByEmail("nonexistent")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> jwtService.refreshAccessToken(httpServletRequest))
                .isInstanceOf(BadTokenException.class);
        }

        @Test
        @DisplayName("Should throw BadTokenException when using access token for refresh")
        void shouldThrowWhenUsingAccessTokenForRefresh() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String accessToken = jwtService.generateAccessToken(TEST_USERNAME);
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + accessToken);
            when(userService.existsByUsername(TEST_USERNAME)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> jwtService.refreshAccessToken(httpServletRequest))
                .isInstanceOf(BadTokenException.class);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should detect tampered token")
        void shouldDetectTamperedToken() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String validToken = jwtService.generateAccessToken(TEST_USERNAME);
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xxxxx";

            // When/Then
            assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("Should reject completely invalid token")
        void shouldRejectCompletelyInvalidToken() {
            // Given
            String invalidToken = "not.a.valid.jwt.token";

            // When/Then
            assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            // When/Then
            assertThatThrownBy(() -> jwtService.extractUsername(""))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should reject null token gracefully in validation")
        void shouldHandleNullTokenInValidation() {
            // Given
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);

            // When/Then
            assertThatThrownBy(() -> jwtService.isValidToken(null, userDetails))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should log warnings for invalid tokens")
        void shouldLogWarningsForInvalidTokens() {
            // Given
            String invalidToken = "invalid.token.here";

            // When
            try {
                jwtService.extractUsername(invalidToken);
            } catch (Exception e) {
                // Expected
            }

            // Then - Verify warning was logged
            verify(logger, atLeastOnce()).warn(anyString());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle username with special characters")
        void shouldHandleUsernameWithSpecialCharacters() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String specialUsername = "user_with-special.chars@123";

            // When
            String token = jwtService.generateAccessToken(specialUsername);
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("Should handle email with plus sign")
        void shouldHandleEmailWithPlusSign() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String emailWithPlus = "test+alias@example.com";

            // When
            String token = jwtService.generateEmailToken(emailWithPlus);
            String extractedEmail = jwtService.extractUsername(token);

            // Then
            assertThat(extractedEmail).isEqualTo(emailWithPlus);
        }

        @Test
        @DisplayName("Should handle long username")
        void shouldHandleLongUsername() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String longUsername = "a".repeat(100);

            // When
            String token = jwtService.generateAccessToken(longUsername);
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Should handle unicode characters in username")
        void shouldHandleUnicodeCharacters() {
            assumeTrue(signingSecretSet, "SIGNING_SECRET not configured");
            
            // Given
            String unicodeUsername = "用户名🎉";

            // When
            String token = jwtService.generateAccessToken(unicodeUsername);
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo(unicodeUsername);
        }
    }
}
