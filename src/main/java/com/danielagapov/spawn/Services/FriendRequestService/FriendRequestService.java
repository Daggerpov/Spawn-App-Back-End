package com.danielagapov.spawn.Services.FriendRequestService;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.FullFriendRequestDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendRequestMapper;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FriendRequestService implements IFriendRequestService {
    private final IFriendRequestsRepository repository;
    private final IUserService userService;
    private final ILogger logger;

    public FriendRequestService(IFriendRequestsRepository repository, IUserService userService, ILogger logger) {
        this.repository = repository;
        this.userService = userService;
        this.logger = logger;
    }

    @Override
    public FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO) {
        try {
            UUID senderId = friendRequestDTO.senderUserId();
            UUID receiverId = friendRequestDTO.receiverUserId();

            if (repository.existsBySenderIdAndReceiverId(senderId, receiverId) ||
                    repository.existsBySenderIdAndReceiverId(receiverId, senderId)) {
                throw new BaseSaveException("A friend request already exists between these users.");
            }

            if (userService.areUsersFriends(senderId, receiverId)) {
                throw new BaseSaveException("Users are already friends.");
            }

            User sender = userService.getUserEntityById(senderId);
            User receiver = userService.getUserEntityById(receiverId);

            FriendRequest friendRequest = FriendRequestMapper.toEntity(friendRequestDTO, sender, receiver);
            repository.save(friendRequest);

            return FriendRequestMapper.toDTO(friendRequest);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save friend request: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FullFriendRequestDTO> getIncomingFriendRequestsByUserId(UUID id) {
        try {
            List<FriendRequest> friendRequests = repository.findByReceiverId(id);

            if (friendRequests.isEmpty()) {
                throw new BaseNotFoundException(EntityType.FriendRequest, id);
            }

            return convertFriendRequestsToFullFriendRequests(FriendRequestMapper.toDTOList(friendRequests));
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseNotFoundException(EntityType.FriendRequest, id);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void acceptFriendRequest(UUID id) {
        FriendRequest fr = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.FriendRequest, id));

        // Check if users are already friends
        if (userService.areUsersFriends(fr.getSender().getId(), fr.getReceiver().getId())) {
            throw new BaseSaveException("Users are already friends.");
        }

        userService.saveFriendToUser(fr.getSender().getId(), fr.getReceiver().getId());
        deleteFriendRequest(id);
    }

    @Override
    public void deleteFriendRequest(UUID id) {
        try {
            if (!repository.existsById(id)) {
                throw new BaseNotFoundException(EntityType.FriendRequest, id);
            }
            repository.deleteById(id);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FullFriendRequestDTO> convertFriendRequestsToFullFriendRequests (List<FriendRequestDTO> friendRequests) {
        List<FullFriendRequestDTO> fullFriendRequests = new ArrayList<>();

        if (friendRequests == null || friendRequests.isEmpty()) {
            return new ArrayList<>();
        }

        for (FriendRequestDTO friendRequest : friendRequests) {
            fullFriendRequests.add(new FullFriendRequestDTO(
                    friendRequest.id(),
                    userService.getFullUserById(friendRequest.senderUserId()),
                    userService.getFullUserById(friendRequest.receiverUserId())
            ));
        }
        return fullFriendRequests;
    }
}
