package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.Enums.UserRelationshipType;
import com.danielagapov.spawn.Models.User.User;

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
