package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.user.api.dto.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.shared.util.UserRelationshipType;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.UUID;

public final class FriendUserMapper {

    public static RecommendedFriendUserDTO toDTO(User user, int mutualCount) {
        return new RecommendedFriendUserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString(),
                mutualCount
        );
    }

    public static RecommendedFriendUserDTO toDTO(User user, int mutualCount, int sharedActivitiesCount) {
        return new RecommendedFriendUserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString(),
                mutualCount,
                sharedActivitiesCount
        );
    }

    public static RecommendedFriendUserDTO toDTO(User user, int mutualCount, int sharedActivitiesCount, UserRelationshipType relationshipStatus, UUID pendingFriendRequestId) {
        return new RecommendedFriendUserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString(),
                mutualCount,
                sharedActivitiesCount,
                relationshipStatus,
                pendingFriendRequestId
        );
    }
}
