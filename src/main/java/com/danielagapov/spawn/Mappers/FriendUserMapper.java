package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.Models.User;

public class FriendUserMapper {

    public static RecommendedFriendUserDTO toDTO(User user, int mutualCount) {
        return new RecommendedFriendUserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString(),
                mutualCount
        );
    }
}
