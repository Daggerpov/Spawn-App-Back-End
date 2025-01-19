package com.danielagapov.spawn.Services.FriendRequestService;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendRequestMapper;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FriendRequestService implements IFriendRequestService {
    private final IFriendRequestsRepository repository;
    private final IUserService userService;
    private final ILogger logger;

    public FriendRequestService(IFriendRequestsRepository repository, IUserService userService,ILogger logger) {
        this.repository = repository;
        this.userService = userService;
        this.logger = logger;
    }

    public FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO) {
        try {
            // Extract sender and receiver IDs from the FriendRequestDTO
            UUID senderId = friendRequestDTO.senderUserId();
            UUID receiverId = friendRequestDTO.receiverUserId();

            User sender = userService.getUserEntityById(senderId);
            User receiver = userService.getUserEntityById(receiverId);

            // Map the DTO to entity
            FriendRequest friendRequest = FriendRequestMapper.toEntity(friendRequestDTO, sender, receiver);

            // Save the friend request
            repository.save(friendRequest);

            // Return the saved friend request DTO with additional details (friends and friend tags)
            return FriendRequestMapper.toDTO(friendRequest, senderFriends, senderFriendTags, receiverFriends, receiverFriendTags);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save friend request: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    public List<FriendRequestDTO> getIncomingFriendRequestsByUserId(UUID id) {
        return List.of();
    }

    public void acceptFriendRequest(UUID id) {
        FriendRequest fr = repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.FriendRequest, id));
        userService.saveFriendToUser(fr.getSender().getId(), fr.getReceiver().getId());
        deleteFriendRequest(id);
    }

    public void deleteFriendRequest(UUID id) {
        repository.deleteById(id);
    }
}
