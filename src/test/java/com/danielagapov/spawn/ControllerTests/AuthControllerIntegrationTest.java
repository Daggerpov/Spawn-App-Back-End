package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.User.*;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Services.Auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth Controller Integration Tests")
public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    private static final String AUTH_BASE_URL = "/api/v1/auth";

    @Autowired
    private AuthService authService;

    @Override
    protected void setupTestData() {
        // Create test data only for specific tests that need it
        // Don't create a generic "testuser" that conflicts with individual tests
    }

    private void createTestUserForQuickSignIn() {
        try {
            // Create a test user specifically for quickSignIn test
            AuthUserDTO testUserDTO = new AuthUserDTO(null, "Test User", "testuser@example.com", "testuser", "Test bio", "password123");
            authService.registerUser(testUserDTO);
        } catch (Exception e) {
            // User might already exist from previous tests, ignore
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should register new user successfully")
    void testRegisterUser_Success() throws Exception {
        AuthUserDTO authUserDTO = new AuthUserDTO(null, "Test User", "test@example.com", "uniquetestuser", "Test bio", "password123");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(authUserDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(header().exists("X-Refresh-Token"))
                .andExpect(jsonPath("$.username").value("uniquetestuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should return conflict for duplicate username")
    void testRegisterUser_DuplicateUsername() throws Exception {
        AuthUserDTO authUserDTO = new AuthUserDTO(null, "Existing User", "existing@example.com", "existinguser", "Bio", "password123");

        // First registration should succeed
        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(authUserDTO)))
                .andExpect(status().isOk());

        // Second registration with same username should fail
        AuthUserDTO duplicateUserDTO = new AuthUserDTO(null, "Different User", "different@example.com", "existinguser", "Bio", "password123");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(duplicateUserDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should login user successfully")
    void testLoginUser_Success() throws Exception {
        // First register a user
        AuthUserDTO registerDTO = new AuthUserDTO(null, "Login User", "login@example.com", "loginuser", "Bio", "password123");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(registerDTO)));

        // Then login with the same credentials
        AuthUserDTO loginDTO = new AuthUserDTO(null, null, null, "loginuser", null, "password123");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(header().exists("X-Refresh-Token"))
                .andExpect(jsonPath("$.username").value("loginuser"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return unauthorized for invalid credentials")
    void testLoginUser_InvalidCredentials() throws Exception {
        AuthUserDTO loginDTO = new AuthUserDTO(null, null, null, "nonexistentuser", null, "wrongpassword");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/sign-in - Should handle OAuth sign-in")
    void testOAuthSignIn() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(AUTH_BASE_URL + "/sign-in")
                .param("idToken", "mock-id-token")
                .param("provider", "google")
                .param("email", "oauth@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/auth/make-user - Should create OAuth user")
    void testMakeOAuthUser() throws Exception {
        UserCreationDTO userCreationDTO = new UserCreationDTO(null, "oauthuser", null, "OAuth User", "Bio", "oauth@example.com");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/make-user")
                .param("idToken", "mock-id-token")
                .param("provider", "google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userCreationDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(header().exists("X-Refresh-Token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh-token - Should refresh access token")
    void testRefreshToken() throws Exception {
        String mockRefreshToken = createMockJwtToken("testuser");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/refresh-token")
                .header(AUTH_HEADER, BEARER_PREFIX + mockRefreshToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh-token - Should return bad request for missing token")
    void testRefreshToken_MissingToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/refresh-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/change-password - Should change password successfully")
    void testChangePassword_Success() throws Exception {
        // First register a user
        AuthUserDTO registerDTO = new AuthUserDTO(null, "Password User", "password@example.com", "passworduser", "Bio", "oldpassword");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(registerDTO)));

        // Change password
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO("oldpassword", "newpassword123");

        String token = createMockJwtToken("passworduser");

        mockMvc.perform(MockMvcRequestBuilders.post(AUTH_BASE_URL + "/change-password")
                .header(AUTH_HEADER, BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(passwordChangeDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/auth/quick-sign-in - Should return user info for valid token")
    void testQuickSignIn() throws Exception {
        createTestUserForQuickSignIn();
        
        String token = createMockJwtToken("testuser");

        mockMvc.perform(MockMvcRequestBuilders.get(AUTH_BASE_URL + "/quick-sign-in")
                .header(AUTH_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/auth/verify-email - Should verify email with valid token")
    void testVerifyEmail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(AUTH_BASE_URL + "/verify-email")
                .param("token", "valid-email-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("verifyAccountPage"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/test-email - Should send test email (deprecated)")
    void testSendTestEmail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(AUTH_BASE_URL + "/test-email"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email sent"));
    }
} 