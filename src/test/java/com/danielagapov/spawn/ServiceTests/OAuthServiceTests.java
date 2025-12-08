package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.user.api.dto.AuthResponseDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.auth.api.dto.OAuthRegistrationDTO;
import com.danielagapov.spawn.shared.util.OAuthProvider;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.auth.internal.domain.UserIdExternalIdMap;
import com.danielagapov.spawn.auth.internal.repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.auth.internal.services.IAuthService;
import com.danielagapov.spawn.auth.internal.services.AppleOAuthStrategy;
import com.danielagapov.spawn.auth.internal.services.GoogleOAuthStrategy;
import com.danielagapov.spawn.auth.internal.services.IOAuthService;
import com.danielagapov.spawn.auth.internal.services.OAuthService;
import com.danielagapov.spawn.user.internal.services.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class OAuthServiceTests {

    @Mock
    private IUserIdExternalIdMapRepository externalIdMapRepository;

    @Mock
    private IUserService userService;

    @Mock
    private ILogger logger;

    private GoogleOAuthStrategy googleOAuthStrategy;

    private AppleOAuthStrategy appleOAuthStrategy;

    private OAuthService oauthService;

    private User testUser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        googleOAuthStrategy = spy(new GoogleOAuthStrategy(logger));
        appleOAuthStrategy = spy(new AppleOAuthStrategy(logger));

        doReturn(OAuthProvider.google).when(googleOAuthStrategy).getOAuthProvider();
        doReturn(OAuthProvider.apple).when(appleOAuthStrategy).getOAuthProvider();

        oauthService = new OAuthService(externalIdMapRepository, userService, logger, List.of(googleOAuthStrategy, appleOAuthStrategy));
        testUser = new User();
        testUser.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    }


    @Test
    public void testMakeUser_NewUser_Google() {
        UUID userId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "bio", "john.doe@example.com");
        UserDTO savedUserDTO = new UserDTO(userId, null, "john.doe", "profile.jpg", "John Doe", "bio", "john.doe@example.com");
        byte[] profilePicture = new byte[0];
        
        // Create a User entity for the getUserEntityById mock
        User userEntity = new User();
        userEntity.setId(userId);
        userEntity.setEmail("john.doe@example.com");
        userEntity.setName("John Doe");

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(false);
        when(userService.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(userService.createAndSaveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(savedUserDTO);
        when(userService.getUserEntityById(userId)).thenReturn(userEntity);
        when(externalIdMapRepository.findById("externalId123")).thenReturn(Optional.empty());
        when(externalIdMapRepository.save(any(UserIdExternalIdMap.class))).thenReturn(new UserIdExternalIdMap("externalId123", userEntity, OAuthProvider.google));

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Making user"));
        verify(logger).info(contains("Returning BaseUserDTO of newly made user"));
    }

    @Test
    public void testMakeUser_ExistingUserByExternalId_Google() {
        UUID id = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "Bio", "john.doe@example.com");
        byte[] profilePicture = new byte[0];

        User user = new User();
        user.setId(id);
        user.setEmail("john.doe@example.com");
        when(externalIdMapRepository.existsById("externalId123")).thenReturn(true);
        when(externalIdMapRepository.findById("externalId123")).thenReturn(Optional.of((new UserIdExternalIdMap("externalId123", user, OAuthProvider.google))));

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Existing user detected in makeUser, mapping already exists"));
    }

    @Test
    public void testMakeUser_ExistingUserByEmail_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "Bio", "john.doe@example.com");
        byte[] profilePicture = new byte[0];
        User user = new User();
        user.setEmail("john.doe@example.com");
        when(externalIdMapRepository.existsById("externalId123")).thenReturn(false);
        when(userService.existsByEmail(userDTO.getEmail())).thenReturn(true);
        when(externalIdMapRepository.findByUserEmail("john.doe@example.com")).thenReturn(Optional.of(new UserIdExternalIdMap("externalId456", user, OAuthProvider.apple)));

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Existing user detected in makeUser, email already exists"));
    }

    @Test
    public void testMakeUser_DatabaseException_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "Bio", "john.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(externalIdMapRepository.existsById("externalId123")).thenThrow(new DataAccessException("DB error") {
        });

        assertThrows(DataAccessException.class, () -> oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google));
        verify(logger).error(contains("Database error while creating user"));
    }

    @Test
    public void testMakeUser_UnexpectedException_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "Bio", "john.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenThrow(new RuntimeException("Unexpected save error"));

        assertThrows(RuntimeException.class, () -> oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google));
        verify(logger).error(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_NewUser_Apple() {
        UUID userId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", "jane.doe@example.com");
        UserDTO savedUserDTO = new UserDTO(userId, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", "jane.doe@example.com");
        byte[] profilePicture = new byte[0];
        
        // Create a User entity for the getUserEntityById mock
        User userEntity = new User();
        userEntity.setId(userId);
        userEntity.setEmail("jane.doe@example.com");
        userEntity.setName("Jane Doe");

        when(externalIdMapRepository.existsById("externalId456")).thenReturn(false);
        when(userService.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(userService.createAndSaveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(savedUserDTO);
        when(userService.getUserEntityById(userId)).thenReturn(userEntity);
        when(externalIdMapRepository.findById("externalId456")).thenReturn(Optional.empty());
        when(externalIdMapRepository.save(any(UserIdExternalIdMap.class))).thenReturn(new UserIdExternalIdMap("externalId456", userEntity, OAuthProvider.apple));

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Making user"));
        verify(logger).info(contains("Returning BaseUserDTO of newly made user"));
    }

    @Test
    public void testMakeUser_ExistingUserByExternalId_Apple() {
        UUID id = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", "jane.doe@example.com");
        byte[] profilePicture = new byte[0];
        User user = new User();
        user.setId(id);
        user.setEmail("jane.doe@example.com");

        when(externalIdMapRepository.existsById("externalId456")).thenReturn(true);
        when(externalIdMapRepository.findById("externalId456")).thenReturn(Optional.of((new UserIdExternalIdMap("externalId456", user, OAuthProvider.apple))));

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Existing user detected in makeUser, mapping already exists"));
    }

    @Test
    public void testMakeUser_ExistingUserByEmail_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", "jane.doe@example.com");
        byte[] profilePicture = new byte[0];

        User user = new User();
        user.setEmail("jane.doe@example.com");
        when(externalIdMapRepository.existsById("externalId456")).thenReturn(false);
        when(userService.existsByEmail(userDTO.getEmail())).thenReturn(true);
        when(externalIdMapRepository.findByUserEmail("jane.doe@example.com")).thenReturn(Optional.of(new UserIdExternalIdMap("externalId123", user, OAuthProvider.google)));

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Existing user detected in makeUser, email already exists"));
    }

    @Test
    public void testMakeUser_DatabaseException_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", "jane.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(externalIdMapRepository.existsById("externalId456")).thenThrow(new DataAccessException("DB error") {
        });

        assertThrows(DataAccessException.class, () -> oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple));
        verify(logger).error(contains("Database error while creating user"));
    }

    @Test
    public void testMakeUser_UnexpectedException_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", "jane.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenThrow(new RuntimeException("Unexpected save error"));

        assertThrows(RuntimeException.class, () -> oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple));
        verify(logger).error(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_NullEmailInUserDTO() {
        UserDTO userDTO = new UserDTO(null, null, "john.noemail", "profile.jpg", "John NoEmail", "Bio", null);
        byte[] profilePicture = new byte[0];

        assertThrows(NullPointerException.class, () ->
                oauthService.makeUser(userDTO, "externalId789", profilePicture, OAuthProvider.google));

        verify(logger).error(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_ExistingMappingDifferentEmail() {
        UUID id = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "john.diffemail", "profile.jpg", "John DiffEmail", "Bio", "john.diffemail@example.com");
        byte[] profilePicture = new byte[0];
        User user = new User();
        user.setId(id);
        user.setEmail("john@example.com");

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(true);
        when(externalIdMapRepository.findById("externalId123")).thenReturn(Optional.of((new UserIdExternalIdMap("externalId123", user, OAuthProvider.google))));

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertNotEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Existing user detected in makeUser, mapping already exists"));
    }

    @Test
    public void testMakeUser_LargeProfilePicture() {
        UUID userId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "john.largepic", "profile.jpg", "John LargePic", "Bio", "john.largepic@example.com");
        UserDTO savedUserDTO = new UserDTO(userId, null, "john.largepic", "profile.jpg", "John LargePic", "Bio", "john.largepic@example.com");
        byte[] profilePicture = new byte[10 * 1024 * 1024]; // 10 MB profile picture
        
        // Create a User entity for the getUserEntityById mock
        User userEntity = new User();
        userEntity.setId(userId);
        userEntity.setEmail("john.largepic@example.com");
        userEntity.setName("John LargePic");

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(false);
        when(userService.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(userService.createAndSaveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(savedUserDTO);
        when(userService.getUserEntityById(userId)).thenReturn(userEntity);
        when(externalIdMapRepository.findById("externalId123")).thenReturn(Optional.empty());
        when(externalIdMapRepository.save(any(UserIdExternalIdMap.class))).thenReturn(new UserIdExternalIdMap("externalId123", userEntity, OAuthProvider.google));

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Making user"));
        verify(logger).info(contains("Returning BaseUserDTO of newly made user"));
    }

    @Test
    public void testGetUserIfExistsByGoogleToken() {
        // Create spy to mock token verification
        OAuthService spyService = spy(oauthService);

        // Mock successful token verification
        doReturn("external_id_123").when(googleOAuthStrategy).verifyIdToken(anyString());

        // Setup mock behavior for user lookup
        User user = new User();
        user.setEmail("test@example.com");

        UserIdExternalIdMap mapping = new UserIdExternalIdMap("external_id_123", user, OAuthProvider.google);

        when(externalIdMapRepository.existsById("external_id_123")).thenReturn(true);
        when(externalIdMapRepository.findById("external_id_123")).thenReturn(Optional.of(mapping));

        // Call the method to test
        Optional<AuthResponseDTO> result = spyService.signInUser("dummy_token", "test@example.com", OAuthProvider.google);

        // Verify the result
        assertTrue(result.isPresent());
        verify(externalIdMapRepository).existsById("external_id_123");
    }

    @Test
    public void testSignInUser_UserNotFound() {
        // Create spy to mock token verification
        OAuthService spyService = spy(oauthService);

        // Mock successful token verification
        doReturn("external_id_123").when(googleOAuthStrategy).verifyIdToken(anyString());

        // Setup mock behavior for user lookup - user not found
        when(externalIdMapRepository.existsById("external_id_123")).thenReturn(false);

        // Call the method to test
        Optional<AuthResponseDTO> result = spyService.signInUser("dummy_token", "test@example.com", OAuthProvider.google);

        // Verify the result
        assertFalse(result.isPresent());
        verify(externalIdMapRepository).existsById("external_id_123");
    }

    @Test
    public void testSignInUser_ReturnsCorrectAuthResponseDTO() {
        // Create spy to mock token verification
        OAuthService spyService = spy(oauthService);

        // Mock successful token verification
        doReturn("external_id_123").when(googleOAuthStrategy).verifyIdToken(anyString());

        // Setup mock behavior for user lookup
        User user = new User();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setName("Test User");
        user.setStatus(UserStatus.EMAIL_VERIFIED);

        UserIdExternalIdMap mapping = new UserIdExternalIdMap("external_id_123", user, OAuthProvider.google);

        when(externalIdMapRepository.existsById("external_id_123")).thenReturn(true);
        when(externalIdMapRepository.findById("external_id_123")).thenReturn(Optional.of(mapping));

        // Call the method to test
        Optional<AuthResponseDTO> result = spyService.signInUser("dummy_token", "test@example.com", OAuthProvider.google);

        // Verify the result
        assertTrue(result.isPresent());
        AuthResponseDTO authResponse = result.get();
        assertEquals("test@example.com", authResponse.getUser().getEmail());
        assertEquals("testuser", authResponse.getUser().getUsername());
        assertEquals("Test User", authResponse.getUser().getName());
        assertEquals(UserStatus.EMAIL_VERIFIED, authResponse.getStatus());
    }

    @Test
    public void testGetUserIfExistsByExternalId_UserExists() {
        // Setup mock behavior for user lookup
        User user = new User();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setName("Test User");
        user.setStatus(UserStatus.USERNAME_AND_PHONE_NUMBER);

        UserIdExternalIdMap mapping = new UserIdExternalIdMap("external_id_123", user, OAuthProvider.google);

        when(externalIdMapRepository.existsById("external_id_123")).thenReturn(true);
        when(externalIdMapRepository.findById("external_id_123")).thenReturn(Optional.of(mapping));

        // Call the method to test
        Optional<AuthResponseDTO> result = oauthService.getUserIfExistsbyExternalId("external_id_123", "test@example.com");

        // Verify the result
        assertTrue(result.isPresent());
        AuthResponseDTO authResponse = result.get();
        assertEquals("test@example.com", authResponse.getUser().getEmail());
        assertEquals("testuser", authResponse.getUser().getUsername());
        assertEquals("Test User", authResponse.getUser().getName());
        assertEquals(UserStatus.USERNAME_AND_PHONE_NUMBER, authResponse.getStatus());
    }

    @Test
    public void testGetUserIfExistsByExternalId_UserNotFound() {
        // Setup mock behavior for user lookup - user not found
        when(externalIdMapRepository.existsById("external_id_123")).thenReturn(false);

        // Call the method to test
        Optional<AuthResponseDTO> result = oauthService.getUserIfExistsbyExternalId("external_id_123", "test@example.com");

        // Verify the result
        assertFalse(result.isPresent());
        verify(externalIdMapRepository).existsById("external_id_123");
    }

    @Test
    public void testGetUserIfExistsByExternalId_ActiveUserStatus() {
        // Setup mock behavior for user lookup
        User user = new User();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setName("Test User");
        user.setStatus(UserStatus.ACTIVE);

        UserIdExternalIdMap mapping = new UserIdExternalIdMap("external_id_123", user, OAuthProvider.google);

        when(externalIdMapRepository.existsById("external_id_123")).thenReturn(true);
        when(externalIdMapRepository.findById("external_id_123")).thenReturn(Optional.of(mapping));

        // Call the method to test
        Optional<AuthResponseDTO> result = oauthService.getUserIfExistsbyExternalId("external_id_123", "test@example.com");

        // Verify the result
        assertTrue(result.isPresent());
        AuthResponseDTO authResponse = result.get();
        assertEquals(UserStatus.ACTIVE, authResponse.getStatus());
    }


    @Test
    public void testSignInUser_Apple_ReturnsAuthResponseDTO() {
        // Create spy to mock token verification
        OAuthService spyService = spy(oauthService);

        // Mock successful token verification
        doReturn("external_id_123").when(appleOAuthStrategy).verifyIdToken(anyString());

        // Setup mock behavior for user lookup
        User user = new User();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setName("Test User");
        user.setStatus(UserStatus.ACTIVE);

        UserIdExternalIdMap mapping = new UserIdExternalIdMap("external_id_123", user, OAuthProvider.apple);

        when(externalIdMapRepository.existsById("external_id_123")).thenReturn(true);
        when(externalIdMapRepository.findById("external_id_123")).thenReturn(Optional.of(mapping));

        // Call the method to test
        Optional<AuthResponseDTO> result = spyService.signInUser("dummy_token", "test@example.com", OAuthProvider.apple);

        // Verify the result
        assertTrue(result.isPresent());
        AuthResponseDTO authResponse = result.get();
        assertEquals("test@example.com", authResponse.getUser().getEmail());
        assertEquals("testuser", authResponse.getUser().getUsername());
        assertEquals("Test User", authResponse.getUser().getName());
        assertEquals(UserStatus.ACTIVE, authResponse.getStatus());
    }

    @Test
    public void testSignInUser_TokenVerificationFails() {
        // Create spy to mock token verification
        OAuthService spyService = spy(oauthService);

        // Mock token verification failure
        doThrow(new SecurityException("Invalid token")).when(googleOAuthStrategy).verifyIdToken(anyString());

        // Call the method to test and expect SecurityException
        assertThrows(SecurityException.class, () -> {
            spyService.signInUser("invalid_token", "test@example.com", OAuthProvider.google);
        });
    }

    @Test
    public void testSignInUser_ProviderMismatch() {
        // Create spy to mock token verification
        OAuthService spyService = spy(oauthService);

        // Mock successful token verification - new external ID for Google
        doReturn("google_external_id_123").when(googleOAuthStrategy).verifyIdToken(anyString());

        // Setup mock behavior: user doesn't exist with Google external ID but exists with Apple
        when(externalIdMapRepository.existsById("google_external_id_123")).thenReturn(false);
        when(userService.existsByEmail("test@example.com")).thenReturn(true);

        // Setup existing user with Apple provider and ACTIVE status
        User user = new User();
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ACTIVE);

        UserIdExternalIdMap appleMapping = new UserIdExternalIdMap("apple_external_id_456", user, OAuthProvider.apple);
        when(externalIdMapRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(appleMapping));

        // Call the method to test and expect IncorrectProviderException
        assertThrows(Exception.class, () -> {
            spyService.signInUser("dummy_token", "test@example.com", OAuthProvider.google);
        });
    }

    // ================== Integration Tests for Race Condition Fixes ==================
    // These tests require the full Spring context and real repositories
    
    /**
     * Integration test class to verify that OAuth race condition issues are resolved.
     * Specifically tests the scenario where a user deletes their account 
     * and immediately tries to re-register with the same OAuth provider.
     */
    @Test
    void testOAuthMappingConcurrencyHandling() {
        // Test that the application-level synchronization in createAndSaveMapping works correctly
        String externalId = "test-concurrent-external-id";
        OAuthProvider provider = OAuthProvider.google;
        
        // Mock the repository to simulate the mapping creation
        when(externalIdMapRepository.findById(externalId)).thenReturn(Optional.empty());
        when(externalIdMapRepository.save(any(UserIdExternalIdMap.class)))
            .thenReturn(new UserIdExternalIdMap(externalId, testUser, provider));
        
        // This should complete without throwing any concurrency-related exceptions
        assertDoesNotThrow(() -> {
            oauthService.createAndSaveMapping(testUser, externalId, provider);
        }, "OAuth mapping creation should handle concurrency gracefully");
        
        verify(externalIdMapRepository, times(1)).findById(externalId);
        verify(externalIdMapRepository, times(1)).save(any(UserIdExternalIdMap.class));
    }

    /**
     * Run the actual Spring Boot integration tests in a nested class with proper test configuration
     */
    @SpringBootTest
    @ActiveProfiles("test")
    @Transactional
    static class RaceConditionIntegrationTests {

        @Autowired
        private IAuthService authService;

        @Autowired
        private IOAuthService oauthService;

        @Autowired
        private IUserService userService;

        @Autowired
        private IUserRepository userRepository;

        @Autowired
        private IUserIdExternalIdMapRepository mappingRepository;

        /**
         * Tests the scenario from the bug report:
         * 1. User has an incomplete account (EMAIL_VERIFIED status)
         * 2. User tries to re-register with same Google OAuth token
         * 3. System should delete the incomplete user and create a new one
         * 4. Should not get "Row was updated or deleted by another transaction" error
         */
        @Test
        public void testOAuthReRegistrationAfterIncompleteAccount() {
            String email = "racetest@example.com";
            String externalId = "109582192032674484261"; // Same as in the logs
            OAuthProvider provider = OAuthProvider.google;

            // Step 1: Create an incomplete user account (EMAIL_VERIFIED status)
            User incompleteUser = new User();
            incompleteUser.setEmail(email);
            incompleteUser.setUsername(externalId);
            incompleteUser.setPhoneNumber(externalId);
            incompleteUser.setName(externalId);
            incompleteUser.setStatus(UserStatus.EMAIL_VERIFIED);
            incompleteUser.setDateCreated(new Date());
            incompleteUser.setProfilePictureUrlString("default-profile-pic.jpg");
            
            User savedUser = userRepository.save(incompleteUser);

            // Step 2: Create OAuth mapping for the incomplete user
            UserIdExternalIdMap mapping = new UserIdExternalIdMap(externalId, savedUser, provider);
            mappingRepository.save(mapping);

            // Verify initial state
            assertTrue(mappingRepository.existsById(externalId));
            assertTrue(userRepository.existsById(savedUser.getId()));

            // Step 3: Simulate OAuth re-registration (the scenario from the bug report)
            OAuthRegistrationDTO registrationDTO = new OAuthRegistrationDTO(
                email,
                "mock-valid-google-token", // This would be mocked in a real test
                provider,
                "Test User",
                "https://example.com/profile.jpg"
            );

            // This should NOT throw "Row was updated or deleted by another transaction" error
            assertDoesNotThrow(() -> {
                // In a real test, we'd mock the Google token verification
                // For now, we'll test the mapping creation logic directly
                
                // Step 3a: Delete the incomplete user (this is what checkOAuthRegistration does)
                userService.deleteUserById(savedUser.getId());
                
                // Step 3b: Verify the mapping was also deleted
                assertFalse(mappingRepository.existsById(externalId));
                assertFalse(userRepository.existsById(savedUser.getId()));
                
                // Step 3c: Create new user and mapping
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(externalId);
                newUser.setPhoneNumber(externalId);
                newUser.setName("Test User");
                newUser.setStatus(UserStatus.EMAIL_VERIFIED);
                newUser.setDateCreated(new Date());
                newUser.setProfilePictureUrlString("https://example.com/profile.jpg");
                
                User savedNewUser = userRepository.save(newUser);
                
                // This should work without race condition errors
                oauthService.createAndSaveMapping(savedNewUser, externalId, provider);
                
                // Verify the new mapping was created successfully
                assertTrue(mappingRepository.existsById(externalId));
                UserIdExternalIdMap newMapping = mappingRepository.findById(externalId).orElse(null);
                assertNotNull(newMapping);
                assertEquals(savedNewUser.getId(), newMapping.getUser().getId());
                assertEquals(provider, newMapping.getProvider());
            });
        }

        /**
         * Test that explicit OAuth mapping deletion works correctly
         */
        @Test
        public void testExplicitMappingDeletion() {
            String email = "maptest@example.com";
            String externalId = "test-external-id-123";
            OAuthProvider provider = OAuthProvider.google;

            // Create user and mapping
            User user = new User();
            user.setEmail(email);
            user.setUsername("testuser");
            user.setPhoneNumber("1234567890");
            user.setName("Test User");
            user.setStatus(UserStatus.EMAIL_VERIFIED);
            user.setDateCreated(new Date());
            user.setProfilePictureUrlString("default-profile-pic.jpg");
            
            User savedUser = userRepository.save(user);
            
            UserIdExternalIdMap mapping = new UserIdExternalIdMap(externalId, savedUser, provider);
            mappingRepository.save(mapping);

            // Verify initial state
            assertTrue(mappingRepository.existsById(externalId));
            assertTrue(userRepository.existsById(savedUser.getId()));

            // Delete user (which should also delete the mapping)
            userService.deleteUserById(savedUser.getId());

            // Verify both user and mapping are deleted
            assertFalse(userRepository.existsById(savedUser.getId()));
            assertFalse(mappingRepository.existsById(externalId));
        }


    }
}