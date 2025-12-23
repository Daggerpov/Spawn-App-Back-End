package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.RecentlySpawnedUserDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.api.dto.UserUpdateDTO;
import com.danielagapov.spawn.activity.api.dto.UserIdActivityTimeDTO;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.shared.exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.social.internal.domain.Friendship;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;
import com.danielagapov.spawn.social.internal.repositories.IFriendshipRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.auth.internal.repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.activity.internal.services.IActivityTypeService;
import com.danielagapov.spawn.social.internal.services.IBlockedUserService;
import com.danielagapov.spawn.social.internal.services.IFriendRequestService;
import com.danielagapov.spawn.media.internal.services.IS3Service;
import com.danielagapov.spawn.user.internal.services.UserService;
import com.danielagapov.spawn.user.internal.services.IUserSearchQueryService;
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
    private IUserSearchQueryService userSearchQueryService;

    @Mock
    private IActivityUserRepository activityUserRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private IActivityTypeService activityTypeService;

    @Mock
    private IUserIdExternalIdMapRepository userIdExternalIdMapRepository;

    @Mock
    private com.danielagapov.spawn.user.internal.services.IUserFriendshipQueryService friendshipQueryService;

    @Spy
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set the adminUsername field since @Value annotation doesn't work in unit tests
        ReflectionTestUtils.setField(userService, "adminUsername", "admin");
        
        // Mock default friendshipQueryService behavior
        when(friendshipQueryService.getAllActiveUsers()).thenReturn(List.of());
        when(friendshipQueryService.getFriendUserIdsByUserId(any())).thenReturn(List.of());
        when(friendshipQueryService.getFriendUsersByUserId(any())).thenReturn(List.of());
        when(friendshipQueryService.getFullFriendUsersByUserId(any())).thenReturn(List.of());
        when(friendshipQueryService.getMutualFriendCount(any(), any())).thenReturn(0);
        when(friendshipQueryService.isUserFriendOfUser(any(), any())).thenReturn(false);
    }

    @Test
    void getAllUsers_ShouldReturnUserDTOs_WhenUsersExist() {
        // Given
        User user1 = createUser(UUID.randomUUID(), "user1", "user1@example.com");
        User user2 = createUser(UUID.randomUUID(), "user2", "user2@example.com");
        List<User> users = Arrays.asList(user1, user2);

        when(friendshipQueryService.getAllActiveUsers()).thenReturn(users);
        when(friendshipQueryService.getFriendUserIdsByUserId(any())).thenReturn(List.of());

        // When
        List<UserDTO> result = userService.getAllUsers();

        // Then
        assertEquals(2, result.size());
        verify(friendshipQueryService, times(1)).getAllActiveUsers();
    }

    @Test
    void getUserById_ShouldReturnUserDTO_WhenUserExists() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, "john_doe", "john.doe@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(friendshipQueryService.getFriendUserIdsByUserId(userId)).thenReturn(List.of());

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
        
        List<UUID> expectedFriendIds = Arrays.asList(friendId1, friendId2);
        when(friendshipQueryService.getFriendUserIdsByUserId(userId)).thenReturn(expectedFriendIds);

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
    void saveFriendToUser_ShouldNotCreateDuplicateFriendship_WhenFriendshipAlreadyExists() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        
        when(friendshipRepository.existsByUserA_IdAndUserB_Id(any(), any())).thenReturn(true);

        // When
        userService.saveFriendToUser(userId, friendId);

        // Then
        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(userService, never()).getUserEntityById(any());
    }

    @Test
    void saveFriendToUser_ShouldNotCreateSelfFriendship_WhenUserIdsAreTheSame() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        userService.saveFriendToUser(userId, userId);

        // Then
        verify(friendshipRepository, never()).save(any(Friendship.class));
        verify(friendshipRepository, never()).existsByUserA_IdAndUserB_Id(any(), any());
    }

    @Test
    void saveFriendToUser_ShouldUseCanonicalOrdering_WhenCreatingFriendship() {
        // Given
        UUID userId1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User user1 = createUser(userId1, "user1", "user1@example.com");
        User user2 = createUser(userId2, "user2", "user2@example.com");
        
        // userId1 < userId2, so canonical order should be userId1, userId2
        when(friendshipRepository.existsByUserA_IdAndUserB_Id(userId1, userId2)).thenReturn(false);
        doReturn(user1).when(userService).getUserEntityById(userId1);
        doReturn(user2).when(userService).getUserEntityById(userId2);

        // When - Call with reverse order
        userService.saveFriendToUser(userId2, userId1);

        // Then - Should still check and save in canonical order
        verify(friendshipRepository).existsByUserA_IdAndUserB_Id(userId1, userId2);
        verify(friendshipRepository).save(argThat(friendship -> 
            friendship.getUserA().getId().equals(userId1) && 
            friendship.getUserB().getId().equals(userId2)
        ));
    }

    @Test
    void getFriendUserIdsByUserId_ShouldReturnEmptyList_WhenUserHasNoFriends() {
        // Given
        UUID userId = UUID.randomUUID();
        when(friendshipQueryService.getFriendUserIdsByUserId(userId)).thenReturn(List.of());

        // When
        List<UUID> result = userService.getFriendUserIdsByUserId(userId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getFriendUserIdsByUserId_ShouldReturnCorrectFriendIds_WhenUserIsBothUserAAndUserB() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID friendId1 = UUID.randomUUID();
        UUID friendId2 = UUID.randomUUID();
        
        List<UUID> expectedFriendIds = Arrays.asList(friendId1, friendId2);
        when(friendshipQueryService.getFriendUserIdsByUserId(userId)).thenReturn(expectedFriendIds);

        // When
        List<UUID> result = userService.getFriendUserIdsByUserId(userId);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(friendId1));
        assertTrue(result.contains(friendId2));
    }

    @Test
    void isUserFriendOfUser_ShouldReturnFalse_WhenUsersAreNotFriends() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        when(friendshipQueryService.isUserFriendOfUser(userId, friendId)).thenReturn(false);

        // When
        boolean result = userService.isUserFriendOfUser(userId, friendId);

        // Then
        assertFalse(result);
    }

    @Test
    void getFriendUsersByUserId_ShouldReturnFriendEntities_WhenFriendshipsExist() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID friendId1 = UUID.randomUUID();
        UUID friendId2 = UUID.randomUUID();
        
        User friend1 = createUser(friendId1, "friend1", "friend1@example.com");
        User friend2 = createUser(friendId2, "friend2", "friend2@example.com");
        
        when(friendshipQueryService.getFriendUsersByUserId(userId))
            .thenReturn(Arrays.asList(friend1, friend2));

        // When
        List<User> result = userService.getFriendUsersByUserId(userId);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getId().equals(friendId1)));
        assertTrue(result.stream().anyMatch(u -> u.getId().equals(friendId2)));
    }

    @Test
    void getMutualFriendCount_ShouldReturnCorrectCount_WhenMutualFriendsExist() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(friendshipQueryService.getMutualFriendCount(userId1, userId2)).thenReturn(2);

        // When
        int result = userService.getMutualFriendCount(userId1, userId2);

        // Then
        assertEquals(2, result);
    }

    @Test
    void getMutualFriendCount_ShouldReturnZero_WhenNoMutualFriendsExist() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(friendshipQueryService.getMutualFriendCount(userId1, userId2)).thenReturn(0);

        // When
        int result = userService.getMutualFriendCount(userId1, userId2);

        // Then
        assertEquals(0, result);
    }

    @Test
    void isUserFriendOfUser_ShouldReturnTrue_WhenUsersAreFriends() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        when(friendshipQueryService.isUserFriendOfUser(userId, friendId)).thenReturn(true);

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
        when(userSearchQueryService.getExcludedUserIds(requestingUserId)).thenReturn(Set.of());

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