package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.auth.api.AuthController;
import com.danielagapov.spawn.auth.api.dto.*;
import com.danielagapov.spawn.user.api.dto.*;
import com.danielagapov.spawn.shared.util.OAuthProvider;
import com.danielagapov.spawn.shared.exceptions.*;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.exceptions.Token.BadTokenException;
import com.danielagapov.spawn.shared.exceptions.Token.TokenNotFoundException;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.auth.internal.services.IAuthService;
import com.danielagapov.spawn.auth.internal.services.IEmailService;
import com.danielagapov.spawn.auth.internal.services.IJWTService;
import com.danielagapov.spawn.auth.internal.services.IOAuthService;
import com.danielagapov.spawn.user.internal.services.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for AuthController
 * Tests authentication, registration, OAuth, token management, and email verification
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    @Mock
    private IOAuthService oauthService;

    @Mock
    private IJWTService jwtService;

    @Mock
    private ILogger logger;

    @Mock
    private IAuthService authService;

    @Mock
    private IEmailService emailService;

    @Mock
    private IUserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;
    private String username;
    private String email;
    private String idToken;
    private BaseUserDTO baseUserDTO;
    private UserDTO userDTO;
    private AuthResponseDTO authResponseDTO;
    private User user;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        
        userId = UUID.randomUUID();
        username = "testuser";
        email = "test@example.com";
        idToken = "test-id-token";
        
        baseUserDTO = new BaseUserDTO(userId, username, "pic.jpg", "Test User");
        userDTO = new UserDTO(userId, List.of(), username, "pic.jpg", "Test User", "bio", email);
        authResponseDTO = new AuthResponseDTO(baseUserDTO, true);
        
        user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(email);
    }

    // MARK: - Sign In Tests

    @Test
    void signIn_ShouldReturnOk_WhenUserExists() throws Exception {
        when(oauthService.signInUser(idToken, email, OAuthProvider.GOOGLE))
                .thenReturn(Optional.of(authResponseDTO));
        when(userService.getUserEntityById(userId)).thenReturn(user);
        when(authService.makeHeadersForTokens(any(User.class))).thenReturn(new HttpHeaders());

        mockMvc.perform(get("/api/v1/auth/sign-in")
                .param("idToken", idToken)
                .param("provider", "GOOGLE")
                .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(userId.toString()));

        verify(oauthService, times(1)).signInUser(idToken, email, OAuthProvider.GOOGLE);
    }

    @Test
    void signIn_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        when(oauthService.signInUser(idToken, email, OAuthProvider.GOOGLE))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/auth/sign-in")
                .param("idToken", idToken)
                .param("provider", "GOOGLE")
                .param("email", email))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).info(contains("User not found"));
    }

    @Test
    void signIn_ShouldReturnConflict_WhenIncorrectProvider() throws Exception {
        when(oauthService.signInUser(idToken, email, OAuthProvider.GOOGLE))
                .thenThrow(new IncorrectProviderException("User registered with different provider"));

        mockMvc.perform(get("/api/v1/auth/sign-in")
                .param("idToken", idToken)
                .param("provider", "GOOGLE")
                .param("email", email))
                .andExpect(status().isConflict());

        verify(logger, times(1)).error(contains("Incorrect provider error"));
    }

    @Test
    void signIn_ShouldReturnUnauthorized_WhenTokenExpired() throws Exception {
        when(oauthService.signInUser(idToken, email, OAuthProvider.GOOGLE))
                .thenThrow(new TokenExpiredException("Token has expired"));

        mockMvc.perform(get("/api/v1/auth/sign-in")
                .param("idToken", idToken)
                .param("provider", "GOOGLE")
                .param("email", email))
                .andExpect(status().isUnauthorized());

        verify(logger, times(1)).error(contains("Token expired"));
    }

    @Test
    void signIn_ShouldReturnServiceUnavailable_WhenProviderUnavailable() throws Exception {
        when(oauthService.signInUser(idToken, email, OAuthProvider.APPLE))
                .thenThrow(new OAuthProviderUnavailableException("Apple OAuth is temporarily unavailable"));

        mockMvc.perform(get("/api/v1/auth/sign-in")
                .param("idToken", idToken)
                .param("provider", "APPLE")
                .param("email", email))
                .andExpect(status().isServiceUnavailable());

        verify(logger, times(1)).error(contains("OAuth provider unavailable"));
    }

    // MARK: - Make User Tests

    @Test
    void makeUser_ShouldReturnOk_WhenUserCreated() throws Exception {
        UserCreationDTO creationDTO = new UserCreationDTO(
            username, "pic.jpg", "Test User", "bio", email, null, null
        );
        
        when(oauthService.createUserFromOAuth(any(UserCreationDTO.class), eq(idToken), eq(OAuthProvider.GOOGLE)))
                .thenReturn(baseUserDTO);
        when(authService.makeHeadersForTokens(username)).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/v1/auth/make-user")
                .param("idToken", idToken)
                .param("provider", "GOOGLE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));

        verify(oauthService, times(1)).createUserFromOAuth(any(UserCreationDTO.class), eq(idToken), eq(OAuthProvider.GOOGLE));
    }

    @Test
    void makeUser_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        UserCreationDTO creationDTO = new UserCreationDTO(
            "", "pic.jpg", "Test User", "bio", email, null, null
        );
        
        when(oauthService.createUserFromOAuth(any(UserCreationDTO.class), eq(idToken), eq(OAuthProvider.GOOGLE)))
                .thenThrow(new IllegalArgumentException("Username cannot be empty"));

        mockMvc.perform(post("/api/v1/auth/make-user")
                .param("idToken", idToken)
                .param("provider", "GOOGLE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creationDTO)))
                .andExpect(status().isBadRequest());

        verify(logger, times(1)).warn(contains("Bad request during user creation"));
    }

    @Test
    void makeUser_ShouldReturnUnauthorized_WhenSecurityError() throws Exception {
        UserCreationDTO creationDTO = new UserCreationDTO(
            username, "pic.jpg", "Test User", "bio", email, null, null
        );
        
        when(oauthService.createUserFromOAuth(any(UserCreationDTO.class), eq(idToken), eq(OAuthProvider.GOOGLE)))
                .thenThrow(new SecurityException("Invalid token"));

        mockMvc.perform(post("/api/v1/auth/make-user")
                .param("idToken", idToken)
                .param("provider", "GOOGLE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creationDTO)))
                .andExpect(status().isUnauthorized());

        verify(logger, times(1)).warn(contains("Security error during user creation"));
    }

    // MARK: - Refresh Token Tests

    @Test
    void refreshToken_ShouldReturnOk_WhenTokenRefreshed() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String newToken = "new-access-token";
        
        when(jwtService.refreshAccessToken(request)).thenReturn(newToken);

        ResponseEntity<String> response = authController.refreshToken(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(newToken, response.getBody());
        assertNotNull(response.getHeaders().get("Authorization"));
        verify(jwtService, times(1)).refreshAccessToken(request);
    }

    @Test
    void refreshToken_ShouldReturnBadRequest_WhenNoToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        when(jwtService.refreshAccessToken(request))
                .thenThrow(new TokenNotFoundException("No refresh token found"));

        ResponseEntity<String> response = authController.refreshToken(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(logger, times(1)).error(contains("No authorization token found"));
    }

    @Test
    void refreshToken_ShouldReturnUnauthorized_WhenBadToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        when(jwtService.refreshAccessToken(request))
                .thenThrow(new BadTokenException("Invalid refresh token"));

        ResponseEntity<String> response = authController.refreshToken(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(logger, times(1)).error(contains("Bad or expired token"));
    }

    // MARK: - Register Tests

    @Test
    void register_ShouldReturnOk_WhenUserRegistered() throws Exception {
        AuthUserDTO authUserDTO = new AuthUserDTO(username, email, "password123", "Test User");
        
        when(authService.registerUser(any(AuthUserDTO.class))).thenReturn(userDTO);
        when(authService.makeHeadersForTokens(username)).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authUserDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));

        verify(authService, times(1)).registerUser(any(AuthUserDTO.class));
    }

    @Test
    void register_ShouldReturnConflict_WhenFieldAlreadyExists() throws Exception {
        AuthUserDTO authUserDTO = new AuthUserDTO(username, email, "password123", "Test User");
        
        when(authService.registerUser(any(AuthUserDTO.class)))
                .thenThrow(new UsernameAlreadyExistsException("Username already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authUserDTO)))
                .andExpect(status().isConflict());

        verify(logger, times(1)).warn(contains("field already exists"));
    }

    // MARK: - Login Tests

    @Test
    void login_ShouldReturnOk_WhenCredentialsValid() throws Exception {
        LoginDTO loginDTO = new LoginDTO(username, "password123");
        
        when(authService.loginUser(username, "password123")).thenReturn(authResponseDTO);
        when(authService.makeHeadersForTokens(username)).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(userId.toString()));

        verify(authService, times(1)).loginUser(username, "password123");
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenBadCredentials() throws Exception {
        LoginDTO loginDTO = new LoginDTO(username, "wrongpassword");
        
        when(authService.loginUser(username, "wrongpassword"))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized());

        verify(logger, times(1)).warn(contains("bad credentials"));
    }

    @Test
    void login_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        LoginDTO loginDTO = new LoginDTO(username, "password123");
        
        when(authService.loginUser(username, "password123"))
                .thenThrow(new BaseNotFoundException(EntityType.User, username));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Entity not found during login"));
    }

    // MARK: - Email Verification Tests

    @Test
    void verifyEmail_ShouldReturnSuccess_WhenTokenValid() {
        String emailToken = "valid-email-token";
        
        when(authService.verifyEmail(emailToken)).thenReturn(true);

        ModelAndView result = authController.verifyEmail(emailToken);

        assertEquals("verifyAccountPage", result.getViewName());
        assertEquals("success", result.getModel().get("status"));
        assertEquals(HttpStatus.OK, result.getStatus());
        verify(authService, times(1)).verifyEmail(emailToken);
    }

    @Test
    void verifyEmail_ShouldReturnExpired_WhenTokenExpired() {
        String emailToken = "expired-email-token";
        
        when(authService.verifyEmail(emailToken)).thenReturn(false);

        ModelAndView result = authController.verifyEmail(emailToken);

        assertEquals("verifyAccountPage", result.getViewName());
        assertEquals("expired", result.getModel().get("status"));
        assertEquals(HttpStatus.OK, result.getStatus());
    }

    @Test
    void verifyEmail_ShouldReturnNotFound_WhenTokenNotFound() {
        String emailToken = "invalid-email-token";
        
        when(authService.verifyEmail(emailToken))
                .thenThrow(new BaseNotFoundException(EntityType.User, emailToken));

        ModelAndView result = authController.verifyEmail(emailToken);

        assertEquals("verifyAccountPage", result.getViewName());
        assertEquals("not_found", result.getModel().get("status"));
        assertEquals(HttpStatus.NOT_FOUND, result.getStatus());
        verify(logger, times(1)).error(contains("Error verifying email"));
    }

    // MARK: - OAuth Registration Tests

    @Test
    void registerViaOAuth_ShouldReturnOk_WhenSuccessful() throws Exception {
        OAuthRegistrationDTO registration = new OAuthRegistrationDTO(
            idToken, OAuthProvider.GOOGLE, email, username, "Test User", "pic.jpg", null
        );
        
        when(authService.registerUserViaOAuth(any(OAuthRegistrationDTO.class))).thenReturn(authResponseDTO);
        when(userService.getUserEntityById(userId)).thenReturn(user);
        when(authService.makeHeadersForTokens(any(User.class))).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/v1/auth/register/oauth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(userId.toString()));

        verify(authService, times(1)).registerUserViaOAuth(any(OAuthRegistrationDTO.class));
    }

    @Test
    void registerViaOAuth_ShouldReturnConflict_WhenAccountExists() throws Exception {
        OAuthRegistrationDTO registration = new OAuthRegistrationDTO(
            idToken, OAuthProvider.GOOGLE, email, username, "Test User", "pic.jpg", null
        );
        
        when(authService.registerUserViaOAuth(any(OAuthRegistrationDTO.class)))
                .thenThrow(new AccountAlreadyExistsException("Account already exists"));

        mockMvc.perform(post("/api/v1/auth/register/oauth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isConflict());

        verify(logger, times(1)).warn(contains("account already exists"));
    }

    @Test
    void registerViaOAuth_ShouldTryGracefulHandling_WhenRegistrationFails() throws Exception {
        OAuthRegistrationDTO registration = new OAuthRegistrationDTO(
            idToken, OAuthProvider.GOOGLE, email, username, "Test User", "pic.jpg", null
        );
        
        when(authService.registerUserViaOAuth(any(OAuthRegistrationDTO.class)))
                .thenThrow(new RuntimeException("Database error"));
        when(authService.handleOAuthRegistrationGracefully(any(OAuthRegistrationDTO.class), any()))
                .thenReturn(authResponseDTO);
        when(userService.getUserEntityById(userId)).thenReturn(user);
        when(authService.makeHeadersForTokens(any(User.class))).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/v1/auth/register/oauth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isOk());

        verify(logger, times(1)).info(contains("graceful handling"));
    }

    // MARK: - Email Verification Flow Tests

    @Test
    void sendEmailVerificationForRegistration_ShouldReturnOk_WhenSuccessful() throws Exception {
        SendEmailVerificationRequestDTO request = new SendEmailVerificationRequestDTO(email);
        EmailVerificationResponseDTO response = new EmailVerificationResponseDTO("Verification code sent");
        
        when(authService.sendEmailVerificationCodeForRegistration(email)).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register/verification/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService, times(1)).sendEmailVerificationCodeForRegistration(email);
    }

    @Test
    void sendEmailVerificationForRegistration_ShouldReturnConflict_WhenEmailExists() throws Exception {
        SendEmailVerificationRequestDTO request = new SendEmailVerificationRequestDTO(email);
        
        when(authService.sendEmailVerificationCodeForRegistration(email))
                .thenThrow(new EmailAlreadyExistsException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register/verification/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(logger, times(1)).warn(contains("Email already exists"));
    }

    @Test
    void verifyEmailAndCreateUser_ShouldReturnOk_WhenCodeValid() throws Exception {
        CheckEmailVerificationRequestDTO request = new CheckEmailVerificationRequestDTO(email, "123456");
        
        when(authService.checkEmailVerificationCode(email, "123456")).thenReturn(authResponseDTO);
        when(authService.makeHeadersForTokens(username)).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/v1/auth/register/verification/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService, times(1)).checkEmailVerificationCode(email, "123456");
    }

    @Test
    void verifyEmailAndCreateUser_ShouldReturnBadRequest_WhenCodeInvalid() throws Exception {
        CheckEmailVerificationRequestDTO request = new CheckEmailVerificationRequestDTO(email, "wrong");
        
        when(authService.checkEmailVerificationCode(email, "wrong"))
                .thenThrow(new EmailVerificationException("Invalid verification code"));

        mockMvc.perform(post("/api/v1/auth/register/verification/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(logger, times(1)).warn(contains("Email verification failed"));
    }

    // MARK: - Change Password Tests

    @Test
    void changePassword_ShouldReturnOk_WhenPasswordChanged() throws Exception {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO("oldpass", "newpass");
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer test-token");
        when(jwtService.extractUsername("test-token")).thenReturn(username);
        when(authService.changePassword(username, "oldpass", "newpass")).thenReturn(true);

        ResponseEntity<?> response = authController.changePassword(passwordChangeDTO, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService, times(1)).changePassword(username, "oldpass", "newpass");
    }

    @Test
    void changePassword_ShouldReturnUnauthorized_WhenCurrentPasswordWrong() throws Exception {
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO("wrongpass", "newpass");
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer test-token");
        when(jwtService.extractUsername("test-token")).thenReturn(username);
        when(authService.changePassword(username, "wrongpass", "newpass")).thenReturn(false);

        ResponseEntity<?> response = authController.changePassword(passwordChangeDTO, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(logger, times(1)).warn(contains("Password change failed"));
    }

    // MARK: - Quick Sign In Tests

    @Test
    void quickSignIn_ShouldReturnOk_WhenTokenValid() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer test-token");
        when(authService.getUserByToken("test-token")).thenReturn(authResponseDTO);

        ResponseEntity<?> response = authController.quickSignIn(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService, times(1)).getUserByToken("test-token");
    }

    @Test
    void quickSignIn_ShouldReturnInternalServerError_WhenTokenInvalid() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(authService.getUserByToken("invalid-token"))
                .thenThrow(new RuntimeException("Invalid token"));

        ResponseEntity<?> response = authController.quickSignIn(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(logger, times(1)).error(contains("Error retrieving user"));
    }

    // MARK: - Update User Details Tests

    @Test
    void updateUserDetails_ShouldReturnOk_WhenSuccessful() throws Exception {
        UpdateUserDetailsDTO dto = new UpdateUserDetailsDTO(userId, username, "New Name", "pic.jpg");
        
        when(authService.updateUserDetails(any(UpdateUserDetailsDTO.class))).thenReturn(baseUserDTO);
        when(userService.getUserEntityById(userId)).thenReturn(user);
        when(authService.makeHeadersForTokens(any(User.class))).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/v1/auth/user/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(authService, times(1)).updateUserDetails(any(UpdateUserDetailsDTO.class));
    }

    @Test
    void updateUserDetails_ShouldReturnConflict_WhenUsernameExists() throws Exception {
        UpdateUserDetailsDTO dto = new UpdateUserDetailsDTO(userId, "existinguser", "New Name", "pic.jpg");
        
        when(authService.updateUserDetails(any(UpdateUserDetailsDTO.class)))
                .thenThrow(new UsernameAlreadyExistsException("Username already exists"));

        mockMvc.perform(post("/api/v1/auth/user/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());

        verify(logger, times(1)).warn(contains("Username already exists"));
    }

    // MARK: - Complete Contact Import Tests

    @Test
    void completeContactImport_ShouldReturnOk_WhenSuccessful() throws Exception {
        when(authService.completeContactImport(userId)).thenReturn(baseUserDTO);

        mockMvc.perform(post("/api/v1/auth/complete-contact-import/{userId}", userId))
                .andExpect(status().isOk());

        verify(authService, times(1)).completeContactImport(userId);
    }

    @Test
    void completeContactImport_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        when(authService.completeContactImport(userId))
                .thenThrow(new BaseNotFoundException(EntityType.User, userId));

        mockMvc.perform(post("/api/v1/auth/complete-contact-import/{userId}", userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("User not found for contact import"));
    }

    // MARK: - Accept TOS Tests

    @Test
    void acceptTermsOfService_ShouldReturnOk_WhenSuccessful() throws Exception {
        when(authService.acceptTermsOfService(userId)).thenReturn(baseUserDTO);

        mockMvc.perform(post("/api/v1/auth/accept-tos/{userId}", userId))
                .andExpect(status().isOk());

        verify(authService, times(1)).acceptTermsOfService(userId);
    }

    @Test
    void acceptTermsOfService_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        when(authService.acceptTermsOfService(userId))
                .thenThrow(new BaseNotFoundException(EntityType.User, userId));

        mockMvc.perform(post("/api/v1/auth/accept-tos/{userId}", userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("User not found for TOS acceptance"));
    }

    // MARK: - Edge Case Tests

    @Test
    void signIn_ShouldHandleMultipleProviders_WhenAppleProvider() throws Exception {
        when(oauthService.signInUser(idToken, email, OAuthProvider.APPLE))
                .thenReturn(Optional.of(authResponseDTO));
        when(userService.getUserEntityById(userId)).thenReturn(user);
        when(authService.makeHeadersForTokens(any(User.class))).thenReturn(new HttpHeaders());

        mockMvc.perform(get("/api/v1/auth/sign-in")
                .param("idToken", idToken)
                .param("provider", "APPLE")
                .param("email", email))
                .andExpect(status().isOk());

        verify(oauthService, times(1)).signInUser(idToken, email, OAuthProvider.APPLE);
    }

    @Test
    void registerViaOAuth_ShouldFallbackToSignIn_WhenAllRecoveryFails() throws Exception {
        OAuthRegistrationDTO registration = new OAuthRegistrationDTO(
            idToken, OAuthProvider.GOOGLE, email, username, "Test User", "pic.jpg", null
        );
        
        when(authService.registerUserViaOAuth(any(OAuthRegistrationDTO.class)))
                .thenThrow(new RuntimeException("Database error"));
        when(authService.handleOAuthRegistrationGracefully(any(OAuthRegistrationDTO.class), any()))
                .thenReturn(null);
        when(oauthService.signInUser(idToken, email, OAuthProvider.GOOGLE))
                .thenReturn(Optional.of(authResponseDTO));
        when(userService.getUserEntityById(userId)).thenReturn(user);
        when(authService.makeHeadersForTokens(any(User.class))).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/v1/auth/register/oauth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isOk());

        verify(logger, times(1)).info(contains("final fallback sign-in"));
    }
}

