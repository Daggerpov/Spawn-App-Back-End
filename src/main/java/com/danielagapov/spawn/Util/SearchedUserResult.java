package com.danielagapov.spawn.Util;

import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;

import java.util.List;

// A special instance of Triple
public class SearchedUserResult extends Triple<List<FetchFriendRequestDTO>, List<RecommendedFriendUserDTO>, List<FullFriendUserDTO>> {
    public SearchedUserResult(List<FetchFriendRequestDTO> friendRequestDTOS, List<RecommendedFriendUserDTO> recommendedFriendUserDTOS, List<FullFriendUserDTO> fullFriendUserDTOS) {
        super(friendRequestDTOS, recommendedFriendUserDTOS, fullFriendUserDTOS);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null check and class check
        SearchedUserResult other = (SearchedUserResult) obj; // Safe cast
        return isListSame(first(), other.first()) && 
               isListSame(second(), other.second()) && 
               isListSame(third(), other.third());
    }

    private <T> boolean isListSame(List<T> list1, List<T> list2) {
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            T o1 = list1.get(i);
            T o2 = list2.get(i);
            if (!o1.equals(o2)) return false;
        }
        return true;
    }
}
