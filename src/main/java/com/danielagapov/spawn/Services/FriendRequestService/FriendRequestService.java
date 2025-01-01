package com.danielagapov.spawn.Services.FriendRequestService;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.FriendRequestMapper;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import org.springframework.dao.DataAccessException;

import java.util.List;

public class FriendRequestService implements IFriendRequestService {
    private final IFriendRequestsRepository repository;

    public FriendRequestService(IFriendRequestsRepository repository) {
        this.repository = repository;
    }

    public FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO,
                                              List<UserDTO> senderFriends,
                                              List<FriendTagDTO> senderFriendTags,
                                              List<UserDTO> receiverFriends,
                                              List<FriendTagDTO> receiverFriendTags) {
        try {
            FriendRequest friendRequest = FriendRequestMapper.toEntity(friendRequestDTO);
            repository.save(friendRequest);
            return FriendRequestMapper.toDTO(friendRequest, senderFriends, senderFriendTags, receiverFriends, receiverFriendTags);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save friend request: " + e.getMessage());
        }
    }
}
