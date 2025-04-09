package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Services.UserFriendTag.UserFriendTagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class UserFriendTagServiceTests {
    @Mock
    private ILogger logger;

    @Mock
    private IUserService userService;

    @Mock
    private IUserFriendTagRepository userFriendTagRepository;

    @InjectMocks
    private UserFriendTagService userFriendTagService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    void getFriendUserIdsByFriendTagId_ShouldReturnEmptyList_WhenNoFriendsFound() {
        UUID tagId = UUID.randomUUID();
        when(userFriendTagRepository.findFriendIdsByTagId(tagId))
                .thenReturn(List.of());

        List<UUID> result = userFriendTagService.getFriendUserIdsByFriendTagId(tagId);

        assertTrue(result.isEmpty());
        verify(userFriendTagRepository, times(1)).findFriendIdsByTagId(tagId);
    }

    @Test
    void getFriendsByFriendTagId_ShouldReturnEmptyList_WhenNoFriendsFound() {
        UUID tagId = UUID.randomUUID();
        when(userFriendTagRepository.findFriendIdsByTagId(tagId))
                .thenReturn(List.of());

        List<UserDTO> result = userFriendTagService.getFriendsByFriendTagId(tagId);

        assertTrue(result.isEmpty());
        verify(userFriendTagRepository, times(1)).findFriendIdsByTagId(tagId);
    }


}
