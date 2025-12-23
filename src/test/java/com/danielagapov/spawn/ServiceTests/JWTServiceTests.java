package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.auth.internal.services.IJWTService;
import com.danielagapov.spawn.auth.internal.services.JWTService;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.exceptions.Token.BadTokenException;
import com.danielagapov.spawn.shared.exceptions.Token.TokenNotFoundException;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.services.IUserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JWTService - Critical security component.
 * Tests token generation, validation, and security features.
 * 
 * Note: These tests use a test-only signing secret passed via constructor injection.
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
    // Test-only signing secret (base64 encoded, 256+ bits for HS256)
    private static final String TEST_SIGNING_SECRET = "dGVzdC1zaWduaW5nLXNlY3JldC1mb3Itand0LXRlc3RpbmctdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaHMyNTY=";

    @BeforeEach
    void setup() {
        // Reset mocks and create fresh service instance
        lenient().doNothing().when(logger).info(anyString());
        lenient().doNothing().when(logger).warn(anyString());
        lenient().doNothing().when(logger).error(anyString());
        
        jwtService = new JWTService(logger, userService, TEST_SIGNING_SECRET);
    }

    @Nested
    @DisplayName("Access Token Generation Tests")
    class AccessTokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT access token for username")
        void shouldGenerateValidAccessToken() {
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
            // When
            String token1 = jwtService.generateAccessToken("user1");
            String token2 = jwtService.generateAccessToken("user2");

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should generate different tokens for same user at different times")
        void shouldGenerateDifferentTokensAtDifferentTimes() throws InterruptedException {
            // When
            String token1 = jwtService.generateAccessToken(TEST_USERNAME);
            Thread.sleep(1100); // JWT timestamps are in seconds, so need > 1 second delay
            String token2 = jwtService.generateAccessToken(TEST_USERNAME);

            // Then - Tokens should be different due to different issuedAt timestamps
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should log token generation")
        void shouldLogTokenGeneration() {
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
            // When - isValidToken catches exceptions and returns false for invalid tokens
            boolean isValid = jwtService.isValidToken(null, userDetails);

            // Then
            assertThat(isValid).isFalse();
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
