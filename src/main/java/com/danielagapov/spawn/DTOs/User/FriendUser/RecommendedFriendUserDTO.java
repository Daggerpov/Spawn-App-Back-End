package com.danielagapov.spawn.DTOs.User.FriendUser;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RecommendedFriendUserDTO extends BaseUserDTO {
    int mutualFriendCount;

    public RecommendedFriendUserDTO(UUID id, String firstName, String lastName, String email, String username, String bio, String profilePicture, int mutualFriendCount) {
        super(id, firstName, lastName, email, username, bio, profilePicture);
        this.mutualFriendCount = mutualFriendCount;
    }
}