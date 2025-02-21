package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.TempUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.OAuth.OAuthService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.util.HashSet;
import java.util.List;
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

    @InjectMocks
    private OAuthService oauthService;

    private TempUserDTO tempUserDTO;
    private UserIdExternalIdMap userIdExternalIdMap;
    private User testUser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        tempUserDTO = new TempUserDTO("externalId123", "John", "Doe", "john.doe@example.com", "picture_url");
        testUser = new User();
        testUser.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        userIdExternalIdMap = new UserIdExternalIdMap("externalId123", testUser, OAuthProvider.google);
    }

    private FullUserDTO createFullUserDTO(String email) {
        return new FullUserDTO(
                UUID.randomUUID(),
                List.of(),
                "username",
                "profilePicture",
                "FirstName",
                "LastName",
                "Bio",
                List.of(),
                email
        );
    }

    @Test
    public void testMakeUser_NewUser_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John", "Doe", "Bio", null, "john.doe@example.com");
        byte[] profilePicture = new byte[0];
        FullUserDTO fullUserDTO = createFullUserDTO(userDTO.email());

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(false);
        when(userService.existsByEmail(userDTO.email())).thenReturn(false);
        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(userDTO);
        when(userService.getFullUserByUser(userDTO, new HashSet<>())).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Making user"));
        verify(logger).log(contains("Returning FullUserDTO of newly made user"));
    }

    @Test
    public void testMakeUser_ExistingUserByExternalId_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John", "Doe", "Bio", null, "john.doe@example.com");
        byte[] profilePicture = new byte[0];
        FullUserDTO fullUserDTO = createFullUserDTO(userDTO.email());

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(true);
        when(userService.getFullUserByEmail(userDTO.email())).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Existing user detected in makeUser, mapping already exists"));
    }

    @Test
    public void testMakeUser_ExistingUserByEmail_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John", "Doe", "Bio", null, "john.doe@example.com");
        byte[] profilePicture = new byte[0];
        FullUserDTO fullUserDTO = createFullUserDTO(userDTO.email());

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(false);
        when(userService.existsByEmail(userDTO.email())).thenReturn(true);
        when(userService.getFullUserByEmail(userDTO.email())).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Existing user detected in makeUser, email already exists"));
    }

    @Test
    public void testMakeUser_DatabaseException_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John", "Doe", "Bio", null, "john.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(externalIdMapRepository.existsById("externalId123")).thenThrow(new DataAccessException("DB error") {
        });

        assertThrows(DataAccessException.class, () -> oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google));
        verify(logger).log(contains("Database error while creating user"));
    }

    @Test
    public void testMakeUser_UnexpectedException_Google() {
        UserDTO userDTO = new UserDTO(null, null, "john.doe", "profile.jpg", "John", "Doe", "Bio", null, "john.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenThrow(new RuntimeException("Unexpected save error"));

        assertThrows(RuntimeException.class, () -> oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google));
        verify(logger).log(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_NewUser_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane", "Doe", "Bio", null, "jane.doe@example.com");
        byte[] profilePicture = new byte[0];
        FullUserDTO fullUserDTO = createFullUserDTO(userDTO.email());

        when(externalIdMapRepository.existsById("externalId456")).thenReturn(false);
        when(userService.existsByEmail(userDTO.email())).thenReturn(false);
        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(userDTO);
        when(userService.getFullUserByUser(userDTO, new HashSet<>())).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Making user"));
        verify(logger).log(contains("Returning FullUserDTO of newly made user"));
    }

    @Test
    public void testMakeUser_ExistingUserByExternalId_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane", "Doe", "Bio", null, "jane.doe@example.com");
        byte[] profilePicture = new byte[0];
        FullUserDTO fullUserDTO = createFullUserDTO(userDTO.email());

        when(externalIdMapRepository.existsById("externalId456")).thenReturn(true);
        when(userService.getFullUserByEmail(userDTO.email())).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Existing user detected in makeUser, mapping already exists"));
    }

    @Test
    public void testMakeUser_ExistingUserByEmail_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane", "Doe", "Bio", null, "jane.doe@example.com");
        byte[] profilePicture = new byte[0];
        FullUserDTO fullUserDTO = createFullUserDTO(userDTO.email());

        when(externalIdMapRepository.existsById("externalId456")).thenReturn(false);
        when(userService.existsByEmail(userDTO.email())).thenReturn(true);
        when(userService.getFullUserByEmail(userDTO.email())).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Existing user detected in makeUser, email already exists"));
    }

    @Test
    public void testMakeUser_DatabaseException_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane", "Doe", "Bio", null, "jane.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(externalIdMapRepository.existsById("externalId456")).thenThrow(new DataAccessException("DB error") {
        });

        assertThrows(DataAccessException.class, () -> oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple));
        verify(logger).log(contains("Database error while creating user"));
    }

    @Test
    public void testMakeUser_UnexpectedException_Apple() {
        UserDTO userDTO = new UserDTO(null, null, "jane.doe", "profile.jpg", "Jane", "Doe", "Bio", null, "jane.doe@example.com");
        byte[] profilePicture = new byte[0];

        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenThrow(new RuntimeException("Unexpected save error"));

        assertThrows(RuntimeException.class, () -> oauthService.makeUser(userDTO, "externalId456", profilePicture, OAuthProvider.apple));
        verify(logger).log(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_NullExternalUserId() {
        UserDTO userDTO = new UserDTO(null, null, "john.null", "profile.jpg", "John", "Null", "Bio", null, "john.null@example.com");
        byte[] profilePicture = new byte[0];
        FullUserDTO fullUserDTO = createFullUserDTO(userDTO.email());

        when(userService.existsByEmail(userDTO.email())).thenReturn(false);
        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(userDTO);
        when(userService.getFullUserByUser(userDTO, new HashSet<>())).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, null, profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Making user"));
        verify(logger).log(contains("Returning FullUserDTO of newly made user"));
    }

    @Test
    public void testMakeUser_NullEmailInUserDTO() {
        UserDTO userDTO = new UserDTO(null, null, "john.noemail", "profile.jpg", "John", "NoEmail", "Bio", null, null);
        byte[] profilePicture = new byte[0];

        Exception exception = assertThrows(NullPointerException.class, () ->
                oauthService.makeUser(userDTO, "externalId789", profilePicture, OAuthProvider.google));

        verify(logger).log(contains("Unexpected error while creating user"));
    }

    @Test
    public void testMakeUser_ExistingMappingDifferentEmail() {
        UserDTO userDTO = new UserDTO(null, null, "john.diffemail", "profile.jpg", "John", "DiffEmail", "Bio", null, "john.diffemail@example.com");
        byte[] profilePicture = new byte[0];
        FullUserDTO fullUserDTO = createFullUserDTO("john.original@example.com");

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(true);
        when(userService.getFullUserByEmail("john.diffemail@example.com")).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertNotEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Existing user detected in makeUser, mapping already exists"));
    }

    @Test
    public void testMakeUser_LargeProfilePicture() {
        UserDTO userDTO = new UserDTO(null, null, "john.largepic", "profile.jpg", "John", "LargePic", "Bio", null, "john.largepic@example.com");
        byte[] profilePicture = new byte[10 * 1024 * 1024]; // 10 MB profile picture
        FullUserDTO fullUserDTO = createFullUserDTO(userDTO.email());

        when(externalIdMapRepository.existsById("externalId123")).thenReturn(false);
        when(userService.existsByEmail(userDTO.email())).thenReturn(false);
        when(userService.saveUserWithProfilePicture(userDTO, profilePicture)).thenReturn(userDTO);
        when(userService.getFullUserByUser(userDTO, new HashSet<>())).thenReturn(fullUserDTO);

        FullUserDTO result = oauthService.makeUser(userDTO, "externalId123", profilePicture, OAuthProvider.google);

        assertNotNull(result);
        assertEquals(userDTO.email(), result.email());
        verify(logger).log(contains("Making user"));
        verify(logger).log(contains("Returning FullUserDTO of newly made user"));
    }
}