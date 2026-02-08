package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.shared.util.SearchedUserResult;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IUserSearchService {
    SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery);

    List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId);

    List<BaseUserDTO> searchByQuery(String searchQuery, UUID requestingUserId);

    Set<UUID> getExcludedUserIds(UUID userId);
}
