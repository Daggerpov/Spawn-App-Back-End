package com.danielagapov.spawn.Utils;

import com.danielagapov.spawn.DTOs.FriendRequest.FullFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.RecommendedFriendUserDTO;
import lombok.AllArgsConstructor;

import java.util.List;

// A special instance of Triple
public class SearchedUserResult extends Triple<List<FullFriendRequestDTO>, List<RecommendedFriendUserDTO>, List<FullFriendUserDTO>> {
    public SearchedUserResult(List<FullFriendRequestDTO> friendRequestDTOS, List<RecommendedFriendUserDTO> recommendedFriendUserDTOS, List<FullFriendUserDTO> fullFriendUserDTOS) {
        super(friendRequestDTOS, recommendedFriendUserDTOS, fullFriendUserDTOS);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof SearchedUserResult))
            return false;
        SearchedUserResult other = (SearchedUserResult)obj;
        return other.first().equals(this.first()) && other.second().equals(this.second()) && other.third().equals(this.third());
    }
}
