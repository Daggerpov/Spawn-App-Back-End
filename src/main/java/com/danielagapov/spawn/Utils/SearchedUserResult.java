package com.danielagapov.spawn.Utils;

import com.danielagapov.spawn.DTOs.FriendRequest.FullFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.RecommendedFriendUserDTO;

import java.util.List;

// A special instance of Triple
public class SearchedUserResult extends Triple<List<FullFriendRequestDTO>, List<RecommendedFriendUserDTO>, List<FullFriendUserDTO>> {
    public SearchedUserResult(List<FullFriendRequestDTO> friendRequestDTOS, List<RecommendedFriendUserDTO> recommendedFriendUserDTOS, List<FullFriendUserDTO> fullFriendUserDTOS) {
        super(friendRequestDTOS, recommendedFriendUserDTOS, fullFriendUserDTOS);
    }
}
