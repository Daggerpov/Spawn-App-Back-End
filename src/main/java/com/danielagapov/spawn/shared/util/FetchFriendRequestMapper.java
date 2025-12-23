package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.social.api.dto.FetchFriendRequestDTO;
import com.danielagapov.spawn.social.api.dto.FetchSentFriendRequestDTO;
import com.danielagapov.spawn.social.internal.domain.FriendRequest;
import com.danielagapov.spawn.user.internal.domain.User;

public final class FetchFriendRequestMapper {
    public static FetchFriendRequestDTO toDTO(FriendRequest friendRequest, int mutualFriendCount) {
        User sender = friendRequest.getSender();
        return new FetchFriendRequestDTO(friendRequest.getId(), UserMapper.toDTO(sender), mutualFriendCount);
    }
    
    public static FetchSentFriendRequestDTO toSentDTO(FriendRequest friendRequest) {
        User receiver = friendRequest.getReceiver();
        return new FetchSentFriendRequestDTO(friendRequest.getId(), UserMapper.toDTO(receiver));
    }
}
