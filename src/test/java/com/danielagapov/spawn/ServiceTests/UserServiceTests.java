package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.RecentlySpawnedUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.DTOs.User.UserUpdateDTO;
import com.danielagapov.spawn.DTOs.UserIdActivityTimeDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.Friendship;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.IFriendshipRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Repositories.User.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.User.UserService;
import com.danielagapov.spawn.Services.UserSearch.IUserSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Order(1)
@Execution(ExecutionMode.CONCURRENT)
public class UserServiceTests {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IFriendshipRepository friendshipRepository;

    @Mock
    private ILogger logger;

    @Mock
    private IFriendRequestService friendRequestService;

    @Mock
    private IBlockedUserService blockedUserService;

    @Mock
    private IS3Service s3Service;

    @Mock
    private IUserSearchService userSearchService;

    @Mock
    private IActivityUserRepository activityUserRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private IActivityTypeService activityTypeService;

    @Mock
    private IUserIdExternalIdMapRepository userIdExternalIdMapRepository;

    @Spy
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set the adminUsername field since @Value annotation doesn't work in unit tests
        ReflectionTestUtils.setField(userService, "adminUsername", "admin");
    }

    @Test
    void getAllUsers_ShouldReturnUserDTOs_WhenUsersExist() {
        // Given
        User user1 = createUser(UUID.randomUUID(), "user1", "user1@example.com");
        User user2 = createUser(UUID.randomUUID(), "user2", "user2@example.com");
        List<User> users = Arrays.asList(user1, user2);

        when(userRepository.findAllUsersByStatus(UserStatus.ACTIVE)).thenReturn(users);
        doReturn(List.of()).when(userService).getFriendUserIdsByUserId(any());

        // When
        List<UserDTO> result = userService.getAllUsers();

        // Then
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAllUsersByStatus(UserStatus.ACTIVE);
    }

    @Test
    void getUserById_ShouldReturnUserDTO_WhenUserExists() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, "john_doe", "john.doe@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doReturn(List.of()).when(userService).getFriendUserIdsByUserId(userId);

        // When
        UserDTO result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("john_doe", result.getUsername());
    }

    @Test
    void saveUser_ShouldThrowBaseSaveException_WhenDataAccessExceptionOccurs() {
        // Given
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), new ArrayList<>(), "john_doe", "profile.jpg", "John Doe", "A bio", "john.doe@example.com");
        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("Database error") {
        });

        // When & Then
        assertThrows(BaseSaveException.class, () -> userService.saveUser(userDTO));
    }

    @Test
    void getFriendUserIdsByUserId_ShouldReturnFriendIds_WhenFriendshipsExist() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID friendId1 = UUID.randomUUID();
        UUID friendId2 = UUID.randomUUID();
        
        User user = createUser(userId, "user", "user@example.com");
        User friend1 = createUser(friendId1, "friend1", "friend1@example.com");
        User friend2 = createUser(friendId2, "friend2", "friend2@example.com");
        
        Friendship friendship1 = new Friendship();
        friendship1.setUserA(user);
        friendship1.setUserB(friend1);
        
        Friendship friendship2 = new Friendship();
        friendship2.setUserA(friend2);
        friendship2.setUserB(user);
        
        when(friendshipRepository.findAllByUserIdBidirectional(userId))
            .thenReturn(Arrays.asList(friendship1, friendship2));

        // When
        List<UUID> result = userService.getFriendUserIdsByUserId(userId);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(friendId1));
        assertTrue(result.contains(friendId2));
    }

    @Test
    void saveFriendToUser_ShouldCreateFriendship_WhenUsersExist() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        User user = createUser(userId, "user", "user@example.com");
        User friend = createUser(friendId, "friend", "friend@example.com");
        
        when(friendshipRepository.existsByUserA_IdAndUserB_Id(any(), any())).thenReturn(false);
        doReturn(user).when(userService).getUserEntityById(userId);
        doReturn(friend).when(userService).getUserEntityById(friendId);

        // When
        userService.saveFriendToUser(userId, friendId);

        // Then
        verify(friendshipRepository, times(1)).save(any(Friendship.class));
    }

    @Test
    void isUserFriendOfUser_ShouldReturnTrue_WhenUsersAreFriends() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        when(friendshipRepository.existsBidirectionally(userId, friendId)).thenReturn(true);

        // When
        boolean result = userService.isUserFriendOfUser(userId, friendId);

        // Then
        assertTrue(result);
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser_WhenValidInput() {
        // Given
        UUID userId = UUID.randomUUID();
        User existingUser = createUser(userId, "oldusername", "user@example.com");
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setBio("New bio");
        updateDTO.setUsername("newusername");
        updateDTO.setName("New Name");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        BaseUserDTO result = userService.updateUser(userId, updateDTO);

        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    void getRecentlySpawnedWithUsers_ShouldReturnUsers_WhenDataExists() {
        // Given
        UUID requestingUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        List<UUID> pastActivityIds = Arrays.asList(UUID.randomUUID());
        List<UserIdActivityTimeDTO> activityParticipants = Arrays.asList(
            new UserIdActivityTimeDTO(otherUserId, OffsetDateTime.now())
        );

        when(activityUserRepository.findPastActivityIdsForUser(eq(requestingUserId), eq(ParticipationStatus.participating), any(), any()))
            .thenReturn(pastActivityIds);
        when(activityUserRepository.findOtherUserIdsByActivityIds(pastActivityIds, requestingUserId, ParticipationStatus.participating))
            .thenReturn(activityParticipants);
        when(userSearchService.getExcludedUserIds(requestingUserId)).thenReturn(Set.of());

        // Mock the getBaseUserById method
        BaseUserDTO mockBaseUserDTO = new BaseUserDTO();
        mockBaseUserDTO.setId(otherUserId);
        doReturn(mockBaseUserDTO).when(userService).getBaseUserById(otherUserId);

        // When
        List<RecentlySpawnedUserDTO> result = userService.getRecentlySpawnedWithUsers(requestingUserId);

        // Then
        assertEquals(1, result.size());
        assertEquals(otherUserId, result.get(0).getUser().getId());
    }

    private User createUser(UUID id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setName(username);
        user.setBio("Bio for " + username);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}