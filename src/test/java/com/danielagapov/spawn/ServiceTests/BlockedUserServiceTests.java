package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.user.api.dto.BlockedUserDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.social.api.dto.FetchFriendRequestDTO;
import com.danielagapov.spawn.user.api.dto.SearchResultUserDTO;
import com.danielagapov.spawn.shared.exceptions.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.user.internal.domain.BlockedUser;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IBlockedUserRepository;
import com.danielagapov.spawn.social.internal.services.BlockedUserService;
import com.danielagapov.spawn.social.internal.services.IFriendRequestService;
import com.danielagapov.spawn.social.internal.repositories.IFriendshipRepository;
import com.danielagapov.spawn.user.internal.services.IUserService;
import com.danielagapov.spawn.shared.util.CacheEvictionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataAccessException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlockedUserServiceTests {

    @Mock private IBlockedUserRepository blockedRepo;
    @Mock private IUserService userService;
    @Mock private IFriendRequestService friendRequestService;
    @Mock private IFriendshipRepository friendshipRepository;
    @Mock private ILogger logger;
    @Mock private CacheEvictionHelper cacheEvictionHelper;

    @InjectMocks private BlockedUserService blockedUserService;

    private UUID blockerId;
    private UUID blockedId;
    private User blocker;
    private User blocked;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        blockerId = UUID.randomUUID();
        blockedId = UUID.randomUUID();
        blocker = new User(); blocker.setId(blockerId);
        blocked = new User(); blocked.setId(blockedId);
    }

    @Test
    void blockUser_ShouldDoNothing_WhenBlockerBlocksThemself() {
        blockedUserService.blockUser(blockerId, blockerId, "Self-block");
        verifyNoInteractions(blockedRepo, userService, friendRequestService);
    }

    @Test
    void blockUser_ShouldSkip_WhenAlreadyBlocked() {
        when(blockedRepo.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(true);

        blockedUserService.blockUser(blockerId, blockedId, "Already blocked");
        verify(blockedRepo, times(1)).existsByBlocker_IdAndBlocked_Id(blockerId, blockedId);
        verifyNoMoreInteractions(blockedRepo);
        verifyNoInteractions(friendRequestService);
    }

    @Test
    void blockUser_ShouldSaveBlockAndCleanup() {
        when(blockedRepo.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(false);
        when(userService.getUserEntityById(blockerId)).thenReturn(blocker);
        when(userService.getUserEntityById(blockedId)).thenReturn(blocked);


        blockedUserService.blockUser(blockerId, blockedId, "Testing");

        verify(blockedRepo).save(any(BlockedUser.class));
    }

    @Test
    void blockUser_ShouldThrowSaveException_OnDBError() {
        when(blockedRepo.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(false);
        when(userService.getUserEntityById(blockerId)).thenReturn(blocker);
        when(userService.getUserEntityById(blockedId)).thenReturn(blocked);

        doThrow(new DataAccessException("DB error") {}).when(blockedRepo).save(any());

        assertThrows(BaseSaveException.class, () ->
                blockedUserService.blockUser(blockerId, blockedId, "Fail save"));
    }

    @Test
    void unblockUser_ShouldDeleteIfExists() {
        BlockedUser block = new BlockedUser();
        when(blockedRepo.findByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(Optional.of(block));

        blockedUserService.unblockUser(blockerId, blockedId);
        verify(blockedRepo).delete(block);
    }

    @Test
    void unblockUser_ShouldDoNothing_IfNoBlockExists() {
        when(blockedRepo.findByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(Optional.empty());

        blockedUserService.unblockUser(blockerId, blockedId);
        verify(blockedRepo, never()).delete(any());
    }

    @Test
    void isBlocked_ShouldReturnTrue_WhenBlocked() {
        when(blockedRepo.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(true);

        assertTrue(blockedUserService.isBlocked(blockerId, blockedId));
    }

    @Test
    void getBlockedUsers_ShouldMapEntitiesToDTOs() {
        BlockedUser user1 = new BlockedUser(UUID.randomUUID(), blocker, new User(UUID.randomUUID(), "a", "", "", "", ""), "r1");
        BlockedUser user2 = new BlockedUser(UUID.randomUUID(), blocker, new User(UUID.randomUUID(), "b", "", "", "", ""), "r2");

        when(blockedRepo.findAllByBlocker_Id(blockerId)).thenReturn(List.of(user1, user2));
        List<BlockedUserDTO> result = blockedUserService.getBlockedUsers(blockerId);

        assertEquals(2, result.size());
    }

    @Test
    void getBlockedUserIds_ShouldReturnBlockedUserIds() {
        User u1 = new User(UUID.randomUUID(), "x", ",", "", "", "");
        User u2 = new User(UUID.randomUUID(), "y", "", "", "", "");

        BlockedUser b1 = new BlockedUser(UUID.randomUUID(), blocker, u1, "why");
        BlockedUser b2 = new BlockedUser(UUID.randomUUID(), blocker, u2, "bc");

        when(blockedRepo.findAllByBlocker_Id(blockerId)).thenReturn(List.of(b1, b2));

        List<UUID> ids = blockedUserService.getBlockedUserIds(blockerId);
        assertEquals(List.of(u1.getId(), u2.getId()), ids);
    }

    // Tests for filterOutBlockedUsers method

    @Test
    void filterOutBlockedUsers_ShouldReturnEmptyList_WhenInputIsEmpty() {
        List<BaseUserDTO> result = blockedUserService.filterOutBlockedUsers(List.of(), blockerId);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterOutBlockedUsers_ShouldReturnEmptyList_WhenInputIsNull() {
        List<BaseUserDTO> result = blockedUserService.filterOutBlockedUsers(null, blockerId);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterOutBlockedUsers_ShouldReturnOriginalList_WhenRequestingUserIdIsNull() {
        BaseUserDTO user1 = new BaseUserDTO(UUID.randomUUID(), "user1", "pic1", "User One", "bio1", "user1@email.com");
        BaseUserDTO user2 = new BaseUserDTO(UUID.randomUUID(), "user2", "pic2", "User Two", "bio2", "user2@email.com");
        List<BaseUserDTO> users = List.of(user1, user2);

        List<BaseUserDTO> result = blockedUserService.filterOutBlockedUsers(users, null);
        assertEquals(users, result);
    }

    @Test
    void filterOutBlockedUsers_ShouldFilterOutBlockedUsers_WhenUserIsBlocked() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID user3Id = UUID.randomUUID();

        BaseUserDTO user1 = new BaseUserDTO(user1Id, "user1", "pic1", "User One", "bio1", "user1@email.com");
        BaseUserDTO user2 = new BaseUserDTO(user2Id, "user2", "pic2", "User Two", "bio2", "user2@email.com");
        BaseUserDTO user3 = new BaseUserDTO(user3Id, "user3", "pic3", "User Three", "bio3", "user3@email.com");
        List<BaseUserDTO> users = List.of(user1, user2, user3);

        // Mock that requesting user has blocked user2
        when(blockedRepo.findAllByBlocker_Id(blockerId)).thenReturn(List.of(
                new BlockedUser(UUID.randomUUID(), blocker, new User(user2Id, "user2", "", "", "", ""), "reason")
        ));

        // Mock that no users have blocked the requesting user
        when(blockedRepo.findAllByBlocked_Id(blockerId)).thenReturn(List.of());

        List<BaseUserDTO> result = blockedUserService.filterOutBlockedUsers(users, blockerId);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getId().equals(user1Id)));
        assertTrue(result.stream().anyMatch(u -> u.getId().equals(user3Id)));
        assertFalse(result.stream().anyMatch(u -> u.getId().equals(user2Id)));
    }

    @Test
    void filterOutBlockedUsers_ShouldFilterOutUsersWhoBlockedRequestingUser() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID user3Id = UUID.randomUUID();

        BaseUserDTO user1 = new BaseUserDTO(user1Id, "user1", "pic1", "User One", "bio1", "user1@email.com");
        BaseUserDTO user2 = new BaseUserDTO(user2Id, "user2", "pic2", "User Two", "bio2", "user2@email.com");
        BaseUserDTO user3 = new BaseUserDTO(user3Id, "user3", "pic3", "User Three", "bio3", "user3@email.com");
        List<BaseUserDTO> users = List.of(user1, user2, user3);

        // Mock that requesting user hasn't blocked anyone
        when(blockedRepo.findAllByBlocker_Id(blockerId)).thenReturn(List.of());

        // Mock that user2 has blocked the requesting user
        User user2Entity = new User(user2Id, "user2", "", "", "", "");
        when(blockedRepo.findAllByBlocked_Id(blockerId)).thenReturn(List.of(
                new BlockedUser(UUID.randomUUID(), user2Entity, blocker, "reason")
        ));

        List<BaseUserDTO> result = blockedUserService.filterOutBlockedUsers(users, blockerId);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getId().equals(user1Id)));
        assertTrue(result.stream().anyMatch(u -> u.getId().equals(user3Id)));
        assertFalse(result.stream().anyMatch(u -> u.getId().equals(user2Id)));
    }

    @Test
    void filterOutBlockedUsers_ShouldWorkWithFetchFriendRequestDTO() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        BaseUserDTO senderUser1 = new BaseUserDTO(user1Id, "user1", "pic1", "User One", "bio1", "user1@email.com");
        BaseUserDTO senderUser2 = new BaseUserDTO(user2Id, "user2", "pic2", "User Two", "bio2", "user2@email.com");

        FetchFriendRequestDTO request1 = new FetchFriendRequestDTO(UUID.randomUUID(), senderUser1, 5);
        FetchFriendRequestDTO request2 = new FetchFriendRequestDTO(UUID.randomUUID(), senderUser2, 3);
        List<FetchFriendRequestDTO> requests = List.of(request1, request2);

        // Mock that requesting user has blocked user2
        when(blockedRepo.findAllByBlocker_Id(blockerId)).thenReturn(List.of(
                new BlockedUser(UUID.randomUUID(), blocker, new User(user2Id, "user2", "", "", "", ""), "reason")
        ));

        // Mock that no users have blocked the requesting user
        when(blockedRepo.findAllByBlocked_Id(blockerId)).thenReturn(List.of());

        List<FetchFriendRequestDTO> result = blockedUserService.filterOutBlockedUsers(requests, blockerId);

        assertEquals(1, result.size());
        assertEquals(user1Id, result.get(0).getSenderUser().getId());
    }

    @Test
    void filterOutBlockedUsers_ShouldReturnOriginalList_WhenNoBlocksExist() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        BaseUserDTO user1 = new BaseUserDTO(user1Id, "user1", "pic1", "User One", "bio1", "user1@email.com");
        BaseUserDTO user2 = new BaseUserDTO(user2Id, "user2", "pic2", "User Two", "bio2", "user2@email.com");
        List<BaseUserDTO> users = List.of(user1, user2);

        // Mock that no blocks exist
        when(blockedRepo.findAllByBlocker_Id(blockerId)).thenReturn(List.of());
        when(blockedRepo.findAllByBlocked_Id(blockerId)).thenReturn(List.of());

        List<BaseUserDTO> result = blockedUserService.filterOutBlockedUsers(users, blockerId);

        assertEquals(2, result.size());
        assertEquals(users, result);
    }

    @Test
    void filterOutBlockedUsers_ShouldReturnOriginalList_WhenFilteringFails() {
        UUID user1Id = UUID.randomUUID();
        BaseUserDTO user1 = new BaseUserDTO(user1Id, "user1", "pic1", "User One", "bio1", "user1@email.com");
        List<BaseUserDTO> users = List.of(user1);

        // Mock that database call fails
        when(blockedRepo.findAllByBlocker_Id(blockerId)).thenThrow(new RuntimeException("Database error"));

        List<BaseUserDTO> result = blockedUserService.filterOutBlockedUsers(users, blockerId);

        // Should return original list when filtering fails
        assertEquals(users, result);
    }
}
