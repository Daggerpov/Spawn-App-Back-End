package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.BlockedUser;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IBlockedUserRepository;
import com.danielagapov.spawn.Services.BlockedUser.BlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
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

    @Mock private ILogger logger;

    @Mock
    private IFriendTagService friendTagService;

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
        when(friendTagService.getFriendTagsByOwnerId(any())).thenReturn(List.of()); // ✅ add this stub

        blockedUserService.blockUser(blockerId, blockedId, "Testing");

        verify(blockedRepo).save(any(BlockedUser.class));
    }

    @Test
    void blockUser_ShouldThrowSaveException_OnDBError() {
        when(blockedRepo.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId)).thenReturn(false);
        when(userService.getUserEntityById(blockerId)).thenReturn(blocker);
        when(userService.getUserEntityById(blockedId)).thenReturn(blocked);
        when(friendTagService.getFriendTagsByOwnerId(any())).thenReturn(List.of());
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
}
