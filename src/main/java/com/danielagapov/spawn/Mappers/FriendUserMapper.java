package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.User.FriendUser.PotentialFriendUserDTO;
import com.danielagapov.spawn.Models.User;

public class FriendUserMapper {

    public static PotentialFriendUserDTO toPotentialFriendUserDTO(User user) {
        return new PotentialFriendUserDTO(
                user.getId(),
                user.getFirstName(),
                user.getBio(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString()
        );
    }
}
