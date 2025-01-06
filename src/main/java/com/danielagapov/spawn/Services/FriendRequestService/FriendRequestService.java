package com.danielagapov.spawn.Services.FriendRequestService;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.FriendRequestMapper;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FriendRequestService implements IFriendRequestService {
    private final IFriendRequestsRepository repository;
    private final IUserService userService;
    private final IFriendTagService friendTagService;

    public FriendRequestService(IFriendRequestsRepository repository, IUserService userService, IFriendTagService friendTagService) {
        this.repository = repository;
        this.userService = userService;
        this.friendTagService = friendTagService;
    }

    public FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO) {
        try {
            // Extract sender and receiver IDs from the FriendRequestDTO
            UUID senderId = friendRequestDTO.sender().id();
            UUID receiverId = friendRequestDTO.receiver().id();

            // Fetch sender and receiver friends and friend tags using UserService
            List<UserDTO> senderFriends = userService.getFriendsByUserId(senderId);
            List<FriendTagDTO> senderFriendTags = friendTagService.getFriendTagsByOwnerId(senderId);
            List<UserDTO> receiverFriends = userService.getFriendsByUserId(receiverId);
            List<FriendTagDTO> receiverFriendTags = friendTagService.getFriendTagsByOwnerId(receiverId);

            // Map the DTO to entity
            FriendRequest friendRequest = FriendRequestMapper.toEntity(friendRequestDTO);

            // Save the friend request
            repository.save(friendRequest);

            // Return the saved friend request DTO with additional details (friends and friend tags)
            return FriendRequestMapper.toDTO(friendRequest, senderFriends, senderFriendTags, receiverFriends, receiverFriendTags);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save friend request: " + e.getMessage());
        }
    }

    public List<FriendRequestDTO> getIncomingFriendRequestsByUserId(UUID id) {
        return List.of();
    }
}
