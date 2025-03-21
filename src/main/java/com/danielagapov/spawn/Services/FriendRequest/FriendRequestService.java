package com.danielagapov.spawn.Services.FriendRequest;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FetchFriendRequestMapper;
import com.danielagapov.spawn.Mappers.FriendRequestMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.Services.PushNotification.PushNotificationService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FriendRequestService implements IFriendRequestService {
    private final IFriendRequestsRepository repository;
    private final IUserService userService;
    private final ILogger logger;
    private final PushNotificationService pushNotificationService;

    public FriendRequestService(IFriendRequestsRepository repository, IUserService userService, ILogger logger, PushNotificationService pushNotificationService) {
        this.repository = repository;
        this.userService = userService;
        this.logger = logger;
        this.pushNotificationService = pushNotificationService;
    }

    @Override
    public CreateFriendRequestDTO saveFriendRequest(CreateFriendRequestDTO friendRequestDTO) {
        try {
            // Extract sender and receiver IDs from the FriendRequestDTO
            UUID senderId = friendRequestDTO.getSenderUserId();
            UUID receiverId = friendRequestDTO.getReceiverUserId();

            User sender = userService.getUserEntityById(senderId);
            User receiver = userService.getUserEntityById(receiverId);

            // Map the DTO to entity
            FriendRequest friendRequest = FriendRequestMapper.toEntity(friendRequestDTO, sender, receiver);

            // Save the friend request
            repository.save(friendRequest);

            // Send push notification to the receiver
            sendNewFriendRequestNotification(sender, receiverId);

            // Return the saved friend request DTO with additional details (friends and friend tags)
            return FriendRequestMapper.toDTO(friendRequest);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save friend request: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FetchFriendRequestDTO> getIncomingFetchFriendRequestsByUserId(UUID id) {
        List<FriendRequest> friendRequests = getIncomingFriendRequestsByUserId(id);

        return friendRequests.stream()
                .map(friendRequest -> FetchFriendRequestMapper.toDTO(friendRequest, userService.getMutualFriendCount(id, friendRequest.getSender().getId())))
                .toList();
    }

    @Override
    public List<CreateFriendRequestDTO> getIncomingCreateFriendRequestsByUserId(UUID id) {
        List<FriendRequest> friendRequests = getIncomingFriendRequestsByUserId(id);
        return FriendRequestMapper.toDTOList(friendRequests);
    }

    private void sendNewFriendRequestNotification(User sender, UUID receiverId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "friend_request");
        data.put("senderId", sender.getId().toString());

        pushNotificationService.sendNotificationToUser(
                receiverId,
                "New Friend Request",
                sender.getUsername() + " sent you a friend request",
                data
        );
    }

    private List<FriendRequest> getIncomingFriendRequestsByUserId(UUID id) {
        try {
            List<FriendRequest> friendRequests = repository.findByReceiverId(id);

            // Return an empty list if no incoming friend requests are found
            if (friendRequests.isEmpty()) {
                return new ArrayList<>();
            }

            return friendRequests;
        } catch (DataAccessException e) {
            logger.error("Database access error while retrieving incoming friend requests for userId: " + id);
            throw e; // Only throw for actual database access issues
        } catch (Exception e) {
            logger.error("Unexpected error while retrieving incoming friend requests for userId: " + id + ", Error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void acceptFriendRequest(UUID id) {
        try {
            FriendRequest fr = repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.FriendRequest, id));
            userService.saveFriendToUser(fr.getSender().getId(), fr.getReceiver().getId());

            // Send push notification to the sender
            Map<String, String> data = new HashMap<>();
            data.put("type", "friend_request_accepted");
            data.put("receiverId", fr.getReceiver().getId().toString());

            pushNotificationService.sendNotificationToUser(
                    fr.getSender().getId(),
                    "Friend Request Accepted",
                    fr.getReceiver().getUsername() + " has accepted your friend request",
                    data
            );

            deleteFriendRequest(id);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteFriendRequest(UUID id) {
        try {
            repository.deleteById(id);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FetchFriendRequestDTO> convertFriendRequestsToFetchFriendRequests(List<CreateFriendRequestDTO> friendRequests) {
        List<FetchFriendRequestDTO> fullFriendRequests = new ArrayList<>();
        for (CreateFriendRequestDTO friendRequest : friendRequests) {
            UUID senderId = friendRequest.getSenderUserId();
            UUID receiverId = friendRequest.getReceiverUserId();
            int mutualFriendCount = userService.getMutualFriendCount(receiverId, senderId);

            fullFriendRequests.add(new FetchFriendRequestDTO(
                    friendRequest.getId(),
                    UserMapper.toDTO(userService.getUserEntityById(senderId)),
                    mutualFriendCount
            ));
        }
        return fullFriendRequests;
    }

    @Override
    public List<CreateFriendRequestDTO> getSentFriendRequestsByUserId(UUID userId) {
        try {
            // Retrieve friend requests sent by the user
            List<FriendRequest> sentRequests = repository.findBySenderId(userId);

            // Return an empty list if no friend requests are found
            if (sentRequests.isEmpty()) {
                return new ArrayList<>();
            }

            // Convert the FriendRequest entities to DTOs before returning
            return FriendRequestMapper.toDTOList(sentRequests);
        } catch (DataAccessException e) {
            logger.error("Database access error while retrieving sent friend requests for userId: " + userId);
            throw e; // Only throw for database access issues
        } catch (Exception e) {
            logger.error("Unexpected error while retrieving sent friend requests for userId: " + userId + ", Error: " + e.getMessage());
            throw e;
        }
    }
}
