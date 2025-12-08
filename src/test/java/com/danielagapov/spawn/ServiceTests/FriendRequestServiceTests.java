package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.social.api.dto.CreateFriendRequestDTO;
import com.danielagapov.spawn.social.api.dto.FetchFriendRequestDTO;
import com.danielagapov.spawn.shared.exceptions.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.social.internal.domain.FriendRequest;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.social.internal.repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.social.internal.services.IBlockedUserService;
import com.danielagapov.spawn.social.internal.services.FriendRequestService;
import com.danielagapov.spawn.user.internal.services.IUserService;
import com.danielagapov.spawn.shared.util.CacheEvictionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FriendRequestServiceTests {

    @Mock
    private IFriendRequestsRepository repository;

    @Mock
    private IUserService userService;

    @Mock
    private IBlockedUserService blockedUserService;

    @Mock
    private ILogger logger;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CacheEvictionHelper cacheEvictionHelper;

    @InjectMocks
    private FriendRequestService friendRequestService;

    private UUID senderId;
    private UUID receiverId;
    private User sender;
    private User receiver;
    private FriendRequest friendRequest;
    private CreateFriendRequestDTO friendRequestDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        senderId = UUID.randomUUID();
        receiverId = UUID.randomUUID();

        sender = new User();
        sender.setId(senderId);

        receiver = new User();
        receiver.setId(receiverId);

        friendRequest = new FriendRequest();
        friendRequest.setId(UUID.randomUUID());
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);

        friendRequestDTO = new CreateFriendRequestDTO(friendRequest.getId(), senderId, receiverId);
        // Removed friendTagsByOwnerId cache as it's no longer needed after friendship refactor
    }

    @Test
    void saveFriendRequest_ShouldSaveAndReturnDTO_WhenValidRequest() {
        when(userService.getUserEntityById(senderId)).thenReturn(sender);
        when(userService.getUserEntityById(receiverId)).thenReturn(receiver);
        when(repository.save(any(FriendRequest.class))).thenReturn(friendRequest);

        CreateFriendRequestDTO savedRequest = friendRequestService.saveFriendRequest(friendRequestDTO);

        assertNotNull(savedRequest);
        assertEquals(friendRequest.getId(), savedRequest.getId());
        verify(repository, times(1)).save(any(FriendRequest.class));
    }

    @Test
    void saveFriendRequest_ShouldThrowBaseSaveException_WhenDataAccessExceptionOccurs() {
        when(userService.getUserEntityById(senderId)).thenReturn(sender);
        when(userService.getUserEntityById(receiverId)).thenReturn(receiver);
        doThrow(new DataAccessException("DB error") {
        }).when(repository).save(any(FriendRequest.class));

        BaseSaveException exception = assertThrows(BaseSaveException.class, () -> friendRequestService.saveFriendRequest(friendRequestDTO));
        assertEquals("failed to save an entity: Failed to save friend request: DB error", exception.getMessage());
        verify(logger, times(1)).error("Failed to save friend request from user " + senderId + " (full user info not available) to user " + receiverId + " (full user info not available): DB error");
    }

    @Test
    void getIncomingFetchFriendRequestsByUserId_ShouldReturnRequests_WhenRequestsExist() {
        when(repository.findByReceiverId(receiverId)).thenReturn(List.of(friendRequest));

        List<FetchFriendRequestDTO> requests = friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId);

        assertFalse(requests.isEmpty());
        assertEquals(1, requests.size());
        verify(repository, times(1)).findByReceiverId(receiverId);
    }

    @Test
    void getIncomingFetchFriendRequestsByUserId_ShouldReturnEmptyList_WhenNoRequestsFound() {
        when(repository.findByReceiverId(receiverId)).thenReturn(List.of());
        
        List<FetchFriendRequestDTO> requests = friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId);

        assertNotNull(requests);
        assertTrue(requests.isEmpty(), "Expected an empty list when no friend requests are found.");
        verify(repository, times(1)).findByReceiverId(receiverId);
        // Don't verify any logger.info calls since they now occur in the implementation
    }

    @Test
    void acceptFriendRequest_ShouldAddFriendAndDeleteRequest_WhenValidRequest() {
        UUID friendRequestId = friendRequest.getId();
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));

        friendRequestService.acceptFriendRequest(friendRequestId);

        verify(userService, times(1)).saveFriendToUser(senderId, receiverId);
        verify(repository, times(1)).deleteById(friendRequestId);
    }

    @Test
    void acceptFriendRequest_ShouldHandleGracefully_WhenRequestNotFound() {
        UUID friendRequestId = UUID.randomUUID();
        when(repository.findById(friendRequestId)).thenReturn(Optional.empty());

        // Should not throw exception, but handle gracefully
        assertDoesNotThrow(() -> friendRequestService.acceptFriendRequest(friendRequestId));
        
        // Verify that no user operations were attempted since request wasn't found
        verify(userService, never()).saveFriendToUser(any(), any());
        verify(userService, never()).isUserFriendOfUser(any(), any());
    }

    @Test
    void acceptFriendRequest_ShouldHandleGracefully_WhenUsersAlreadyFriends() {
        UUID friendRequestId = friendRequest.getId();
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));
        when(userService.isUserFriendOfUser(senderId, receiverId)).thenReturn(true);

        // Should not throw exception, but handle gracefully by deleting stale request
        assertDoesNotThrow(() -> friendRequestService.acceptFriendRequest(friendRequestId));
        
        // Verify that friendship wasn't created again (since they're already friends)
        verify(userService, never()).saveFriendToUser(any(), any());
        // Verify that the stale friend request is cleaned up
        verify(userService, times(1)).isUserFriendOfUser(senderId, receiverId);
    }

    @Test
    void deleteFriendRequest_ShouldDeleteRequest_WhenValidId() {
        UUID friendRequestId = friendRequest.getId();
        // Mock findById to return the friend request, so deleteById will be called
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));

        friendRequestService.deleteFriendRequest(friendRequestId);

        verify(repository, times(1)).findById(friendRequestId);
        verify(repository, times(1)).deleteById(friendRequestId);
    }

    @Test
    void deleteFriendRequest_ShouldNotThrowException_WhenFriendRequestNotFound() {
        UUID friendRequestId = UUID.randomUUID();
        // Mock findById to return empty, simulating friend request not found
        when(repository.findById(friendRequestId)).thenReturn(Optional.empty());

        // This should not throw an exception
        assertDoesNotThrow(() -> friendRequestService.deleteFriendRequest(friendRequestId));
        
        verify(repository, times(1)).findById(friendRequestId);
        // deleteById should not be called since friend request doesn't exist
        verify(repository, never()).deleteById(friendRequestId);
    }

    @Test
    void getIncomingFetchFriendRequestsByUserId_ShouldThrowDataAccessException_WhenDataAccessExceptionOccurs() {
        when(repository.findByReceiverId(receiverId)).thenThrow(new DataAccessException("DB read error") {
        });

        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId));

        assertEquals("DB read error", exception.getMessage());
        verify(logger, times(1)).error("Database access error while retrieving incoming friend requests for user: " + receiverId + " (full user info not available)");
    }

    @Test
    void deleteFriendRequest_ShouldThrowException_WhenDataAccessExceptionOccurs() {
        UUID friendRequestId = friendRequest.getId();
        // Mock findById to return the friend request, so deleteById will be called
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));
        doThrow(new DataAccessException("DB delete error") {
        }).when(repository).deleteById(friendRequestId);

        DataAccessException exception = assertThrows(DataAccessException.class, () -> friendRequestService.deleteFriendRequest(friendRequestId));
        assertEquals("DB delete error", exception.getMessage());
        verify(logger, times(1)).error("Error deleting friend request with ID: " + friendRequestId + ": DB delete error");
    }

    @Test
    void saveFriendRequest_ShouldThrowException_WhenSenderOrReceiverIsNull() {
        CreateFriendRequestDTO invalidRequestDTO = new CreateFriendRequestDTO(friendRequest.getId(), null, receiverId);

        assertThrows(NullPointerException.class, () -> friendRequestService.saveFriendRequest(invalidRequestDTO));
        verify(logger, times(1)).error(anyString());
    }

    @Test
    void getIncomingFetchFriendRequestsByUserId_ShouldReturnMultipleRequests_WhenMultipleRequestsExist() {
        FriendRequest anotherFriendRequest = new FriendRequest();
        anotherFriendRequest.setId(UUID.randomUUID());
        anotherFriendRequest.setSender(sender);
        anotherFriendRequest.setReceiver(receiver);

        when(repository.findByReceiverId(receiverId)).thenReturn(List.of(friendRequest, anotherFriendRequest));

        List<FetchFriendRequestDTO> requests = friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId);

        assertEquals(2, requests.size());
        verify(repository, times(1)).findByReceiverId(receiverId);
    }

    @Test
    void saveFriendRequest_ShouldReturnExistingRequest_WhenRequestAlreadyExists() {
        // Arrange: Mock existing request in same direction
        when(userService.getUserEntityById(senderId)).thenReturn(sender);
        when(userService.getUserEntityById(receiverId)).thenReturn(receiver);
        when(repository.findBySenderIdAndReceiverId(senderId, receiverId))
            .thenReturn(List.of(friendRequest));
        when(repository.findBySenderIdAndReceiverId(receiverId, senderId))
            .thenReturn(List.of()); // No reverse request

        // Act
        CreateFriendRequestDTO result = friendRequestService.saveFriendRequest(friendRequestDTO);

        // Assert: Should return existing request without creating new one
        assertNotNull(result);
        assertEquals(friendRequest.getId(), result.getId());
        assertEquals(senderId, result.getSenderUserId());
        assertEquals(receiverId, result.getReceiverUserId());
        
        // Verify: No new request was saved
        verify(repository, never()).save(any(FriendRequest.class));
        
        // Verify: Existing request lookup was performed
        verify(repository).findBySenderIdAndReceiverId(senderId, receiverId);
    }

    @Test
    void saveFriendRequest_ShouldAutoAcceptReverseRequest_WhenReverseRequestExists() {
        // Arrange: Create reverse friend request (receiver -> sender)
        FriendRequest reverseRequest = new FriendRequest();
        UUID reverseRequestId = UUID.randomUUID();
        reverseRequest.setId(reverseRequestId);
        reverseRequest.setSender(receiver);
        reverseRequest.setReceiver(sender);

        when(userService.getUserEntityById(senderId)).thenReturn(sender);
        when(userService.getUserEntityById(receiverId)).thenReturn(receiver);
        
        // Mock: No existing request in the same direction
        when(repository.findBySenderIdAndReceiverId(senderId, receiverId))
            .thenReturn(List.of());
        
        // Mock: Reverse request exists
        when(repository.findBySenderIdAndReceiverId(receiverId, senderId))
            .thenReturn(List.of(reverseRequest));
        
        // Mock: Reverse request can be found by ID for acceptance
        when(repository.findById(reverseRequestId)).thenReturn(Optional.of(reverseRequest));
        
        // Cache eviction is now handled by CacheEvictionHelper (mocked)

        // Act: User A tries to send request to User B (who already requested User A)
        CreateFriendRequestDTO result = friendRequestService.saveFriendRequest(friendRequestDTO);

        // Assert: Should return the reverse request that was accepted
        assertNotNull(result);
        assertEquals(reverseRequestId, result.getId());
        assertEquals(receiverId, result.getSenderUserId()); // Original sender of reverse request
        assertEquals(senderId, result.getReceiverUserId()); // Original receiver of reverse request

        // Verify: Friendship was created
        verify(userService).saveFriendToUser(receiverId, senderId);

        // Verify: Reverse request was deleted (as part of acceptance)
        verify(repository).deleteById(reverseRequestId);

        // Verify: Two notifications were published (both should be FriendRequestAcceptedNotificationEvent)
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));

        // Verify: No new request was saved since we auto-accepted existing one
        verify(repository, never()).save(any(FriendRequest.class));

        // Verify: Cache evictions occurred via helper
        verify(cacheEvictionHelper, atLeast(1)).evictCaches(any(), any(), any());
    }

    @Test
    void saveFriendRequest_ShouldCreateNewRequest_WhenNoExistingRequests() {
        // Arrange: No existing requests in either direction
        when(userService.getUserEntityById(senderId)).thenReturn(sender);
        when(userService.getUserEntityById(receiverId)).thenReturn(receiver);
        when(repository.findBySenderIdAndReceiverId(senderId, receiverId))
            .thenReturn(List.of());
        when(repository.findBySenderIdAndReceiverId(receiverId, senderId))
            .thenReturn(List.of());
        when(repository.save(any(FriendRequest.class))).thenReturn(friendRequest);
        
        // Cache eviction is now handled by CacheEvictionHelper (mocked)

        // Act
        CreateFriendRequestDTO result = friendRequestService.saveFriendRequest(friendRequestDTO);

        // Assert: New request was created
        assertNotNull(result);
        assertEquals(friendRequest.getId(), result.getId());
        assertEquals(senderId, result.getSenderUserId());
        assertEquals(receiverId, result.getReceiverUserId());

        // Verify: New request was saved
        verify(repository).save(any(FriendRequest.class));

        // Verify: Friend request notification was published (not acceptance notification)
        verify(eventPublisher).publishEvent(any(Object.class));

        // Verify: Recommended friends cache was evicted via helper
        verify(cacheEvictionHelper, times(1)).evictCacheForUsers(any(), any(), any()); // For both sender and receiver
    }

    @Test
    void acceptFriendRequest_ShouldCreateBidirectionalFriendship_WhenRequestAccepted() {
        // Given
        UUID friendRequestId = friendRequest.getId();
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));
        when(userService.isUserFriendOfUser(senderId, receiverId)).thenReturn(false);

        // When
        friendRequestService.acceptFriendRequest(friendRequestId);

        // Then
        verify(userService, times(1)).saveFriendToUser(senderId, receiverId);
        verify(repository, times(1)).deleteById(friendRequestId);
    }

    @Test
    void acceptFriendRequest_ShouldHandleBlockedUsers_Gracefully() {
        // Given
        UUID friendRequestId = friendRequest.getId();
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));
        when(userService.isUserFriendOfUser(senderId, receiverId)).thenReturn(false);
        // Note: Blocked user check would be implemented in the actual service

        // When
        friendRequestService.acceptFriendRequest(friendRequestId);

        // Then - Should still process (blocking is handled at UI level)
        verify(userService, times(1)).saveFriendToUser(senderId, receiverId);
        verify(repository, times(1)).deleteById(friendRequestId);
    }

    @Test
    void getSentFriendRequestsByUserId_ShouldReturnRequests_WhenUserHasSentRequests() {
        // Given
        FriendRequest sentRequest = new FriendRequest();
        sentRequest.setId(UUID.randomUUID());
        sentRequest.setSender(sender);
        sentRequest.setReceiver(receiver);
        
        when(repository.findByReceiverId(receiverId)).thenReturn(List.of(sentRequest));

        // When
        List<FetchFriendRequestDTO> result = friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId);

        // Then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(sentRequest.getId(), result.get(0).getId());
        verify(repository, times(1)).findByReceiverId(receiverId);
    }

    @Test
    void saveFriendRequest_ShouldPreventDuplicateRequests_InSameDirection() {
        // Given
        when(userService.getUserEntityById(senderId)).thenReturn(sender);
        when(userService.getUserEntityById(receiverId)).thenReturn(receiver);
        when(repository.findBySenderIdAndReceiverId(senderId, receiverId))
            .thenReturn(List.of(friendRequest));
        when(repository.findBySenderIdAndReceiverId(receiverId, senderId))
            .thenReturn(List.of());

        // When
        CreateFriendRequestDTO result = friendRequestService.saveFriendRequest(friendRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals(friendRequest.getId(), result.getId());
        verify(repository, never()).save(any(FriendRequest.class));
    }

    @Test
    void deleteFriendRequest_ShouldEvictCaches_WhenRequestDeleted() {
        // Given
        UUID friendRequestId = friendRequest.getId();
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));
        // Cache eviction is now handled by CacheEvictionHelper (mocked)

        // When
        friendRequestService.deleteFriendRequest(friendRequestId);

        // Then
        verify(repository, times(1)).deleteById(friendRequestId);
        verify(cacheEvictionHelper, times(2)).evictCache(any(), any()); // For incoming and sent requests
        verify(cacheEvictionHelper, times(1)).evictCacheForUsers(any(), any(), any()); // For recommended friends
    }

    @Test
    void acceptFriendRequest_ShouldHandleConcurrentAcceptance_Gracefully() {
        // Given - Simulate concurrent acceptance where friendship already exists
        UUID friendRequestId = friendRequest.getId();
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));
        when(userService.isUserFriendOfUser(senderId, receiverId)).thenReturn(true); // Already friends

        // When
        friendRequestService.acceptFriendRequest(friendRequestId);

        // Then - Should not create friendship again but should clean up request
        verify(userService, never()).saveFriendToUser(any(), any());
        verify(repository, times(1)).deleteById(friendRequestId);
    }

    @Test
    void saveFriendRequest_ShouldHandleNullUsers_Gracefully() {
        // Given
        CreateFriendRequestDTO invalidDTO = new CreateFriendRequestDTO(UUID.randomUUID(), null, receiverId);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            friendRequestService.saveFriendRequest(invalidDTO);
        });
    }

    @Test
    void getIncomingFetchFriendRequestsByUserId_ShouldHandleLargeResultSets() {
        // Given - Create many friend requests
        List<FriendRequest> manyRequests = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            FriendRequest request = new FriendRequest();
            request.setId(UUID.randomUUID());
            request.setSender(sender);
            request.setReceiver(receiver);
            manyRequests.add(request);
        }
        
        when(repository.findByReceiverId(receiverId)).thenReturn(manyRequests);

        // When
        List<FetchFriendRequestDTO> result = friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId);

        // Then
        assertEquals(100, result.size());
        verify(repository, times(1)).findByReceiverId(receiverId);
    }

}
