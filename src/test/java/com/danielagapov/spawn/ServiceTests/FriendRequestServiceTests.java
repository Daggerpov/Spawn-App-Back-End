package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.Services.FriendRequest.FriendRequestService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
    private ILogger logger;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

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
    }

    @Test
    @Disabled("will be resolved after notification refactor")
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
        verify(logger, times(1)).error("DB error");
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
        verify(logger, times(0)).info(anyString()); // No logging since it's not an error anymore
    }

    @Test
    @Disabled("will be resolved after notification refactor")
    void acceptFriendRequest_ShouldAddFriendAndDeleteRequest_WhenValidRequest() {
        UUID friendRequestId = friendRequest.getId();
        when(repository.findById(friendRequestId)).thenReturn(Optional.of(friendRequest));

        friendRequestService.acceptFriendRequest(friendRequestId);

        verify(userService, times(1)).saveFriendToUser(senderId, receiverId);
        verify(repository, times(1)).deleteById(friendRequestId);
    }

    @Test
    void acceptFriendRequest_ShouldThrowBaseNotFoundException_WhenRequestNotFound() {
        UUID friendRequestId = UUID.randomUUID();
        when(repository.findById(friendRequestId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class, () -> friendRequestService.acceptFriendRequest(friendRequestId));
        assertEquals("FriendRequest entity not found with ID: " + friendRequestId, exception.getMessage());
    }

    @Test
    void deleteFriendRequest_ShouldDeleteRequest_WhenValidId() {
        UUID friendRequestId = friendRequest.getId();

        friendRequestService.deleteFriendRequest(friendRequestId);

        verify(repository, times(1)).deleteById(friendRequestId);
    }

    @Test
    void getIncomingFetchFriendRequestsByUserId_ShouldThrowDataAccessException_WhenDataAccessExceptionOccurs() {
        when(repository.findByReceiverId(receiverId)).thenThrow(new DataAccessException("DB read error") {
        });

        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId));

        assertEquals("DB read error", exception.getMessage());
        verify(logger, times(1)).error("Database access error while retrieving incoming friend requests for userId: " + receiverId);
    }


    @Test
    void deleteFriendRequest_ShouldThrowException_WhenDataAccessExceptionOccurs() {
        UUID friendRequestId = friendRequest.getId();
        doThrow(new DataAccessException("DB delete error") {
        }).when(repository).deleteById(friendRequestId);

        DataAccessException exception = assertThrows(DataAccessException.class, () -> friendRequestService.deleteFriendRequest(friendRequestId));
        assertEquals("DB delete error", exception.getMessage());
        verify(logger, times(1)).error("DB delete error");
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

}
