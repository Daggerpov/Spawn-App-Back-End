package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.User.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.User.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.OAuth.AppleOAuthStrategy;
import com.danielagapov.spawn.Services.OAuth.GoogleOAuthStrategy;
import com.danielagapov.spawn.Services.OAuth.OAuthService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "bio", null, "john.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(false);
        when(userService.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(userService.createAndSaveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(userDTO);

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Making user"));
        verify(logger).info(contains("Returning BaseUserDTO of newly made user"));
    }

    @Test
    public void testMakeUser_ExistingUserByExternalId_Google() {
        UUID id = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "Bio", null, "john.doe@example.com");
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
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "Bio", null, "john.doe@example.com");
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
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "Bio", null, "john.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(externalIdMapRepository.existsById("externalId123")).thenThrow(new DataAccessException("DB error") {
        });

        assertThrows(DataAccessException.class, () -> oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google));
        verify(logger).error(contains("Database error while creating user"));
    }

    @Test
    public void testMakeUser_UnexpectedException_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John Doe", "Bio", null, "john.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenThrow(new RuntimeException("Unexpected save error"));

        assertThrows(RuntimeException.class, () -> oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google));
        verify(logger).error(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_NewUser_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", null, "jane.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(externalIdMapRepository.existsById("externalId456")).thenReturn(false);
        when(userService.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(userService.createAndSaveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(userDTO);

        BaseUserDTO result = oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple);

        assertNotNull(result);
        assertEquals(userDTO.getEmail(), result.getEmail());
        verify(logger).info(contains("Making user"));
        verify(logger).info(contains("Returning BaseUserDTO of newly made user"));
    }

    @Test
    public void testMakeUser_ExistingUserByExternalId_Apple() {
        UUID id = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", null, "jane.doe@example.com");
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
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", null, "jane.doe@example.com");
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
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", null, "jane.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(externalIdMapRepository.existsById("externalId456")).thenThrow(new DataAccessException("DB error") {
        });

        assertThrows(DataAccessException.class, () -> oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple));
        verify(logger).error(contains("Database error while creating user"));
    }

    @Test
    public void testMakeUser_UnexpectedException_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane Doe", "Bio", null, "jane.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenThrow(new RuntimeException("Unexpected save error"));

        assertThrows(RuntimeException.class, () -> oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple));
        verify(logger).error(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_NullEmailInUserDTO() {
        UserDTO userDTO = new UserDTO(null, null, "john.noemail", "profile.jpg", "John NoEmail", "Bio", null, null);
        byte[] profilePicture = new byte[0];

        assertThrows(NullPointerException.class, () ->
                oauthService.makeUser(userDTO, "externalId789", profilePicture, OAuthProvider.google));

        verify(logger).error(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_ExistingMappingDifferentEmail() {
        UUID id = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(null, null, "john.diffemail", "profile.jpg", "John DiffEmail", "Bio", null, "john.diffemail@example.com");
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
        UserDTO userDTO = new UserDTO(null, null, "john.largepic", "profile.jpg", "John LargePic", "Bio", null, "john.largepic@example.com");
        byte[] profilePicture = new byte[10 * 1024 * 1024]; // 10 MB profile picture

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(false);
        when(userService.existsByEmail(userDTO.getEmail())).thenReturn(false);
        when(userService.createAndSaveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(userDTO);

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
        Optional<BaseUserDTO> result = spyService.signInUser("dummy_token", "test@example.com", OAuthProvider.google);

        // Verify the result
        assertTrue(result.isPresent());
        verify(externalIdMapRepository).existsById("external_id_123");
    }
}