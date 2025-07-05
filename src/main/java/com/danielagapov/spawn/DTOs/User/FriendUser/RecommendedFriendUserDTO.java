package com.danielagapov.spawn.DTOs.User.FriendUser;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RecommendedFriendUserDTO extends BaseUserDTO {
    int mutualFriendCount;
    int sharedActivitiesCount;

    public RecommendedFriendUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture, int mutualFriendCount, int sharedActivitiesCount) {
        super(id, name, email, username, bio, profilePicture);
        this.mutualFriendCount = mutualFriendCount;
        this.sharedActivitiesCount = sharedActivitiesCount;
    }
    
    // Legacy constructor for backward compatibility
    public RecommendedFriendUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture, int mutualFriendCount) {
        this(id, name, email, username, bio, profilePicture, mutualFriendCount, 0);
    }
}