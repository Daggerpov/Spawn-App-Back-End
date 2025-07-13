package com.danielagapov.spawn.Services.UserSearch;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.Util.SearchedUserResult;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IUserSearchService {
    SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery);

    List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId);

    List<BaseUserDTO> searchByQuery(String searchQuery, UUID requestingUserId);

    Set<UUID> getExcludedUserIds(UUID userId);
}
