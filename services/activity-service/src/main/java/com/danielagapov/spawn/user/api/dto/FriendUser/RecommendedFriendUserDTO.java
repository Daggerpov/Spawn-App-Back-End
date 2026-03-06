package com.danielagapov.spawn.user.api.dto.FriendUser;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.util.UserRelationshipType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RecommendedFriendUserDTO extends BaseUserDTO {
    int mutualFriendCount;
    int sharedActivitiesCount;
    UserRelationshipType relationshipStatus;
    UUID pendingFriendRequestId;

    public RecommendedFriendUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture, int mutualFriendCount, int sharedActivitiesCount, UserRelationshipType relationshipStatus, UUID pendingFriendRequestId) {
        super(id, name, email, username, bio, profilePicture);
        this.mutualFriendCount = mutualFriendCount;
        this.sharedActivitiesCount = sharedActivitiesCount;
        this.relationshipStatus = relationshipStatus;
        this.pendingFriendRequestId = pendingFriendRequestId;
    }
    
    // Constructor without relationship status for backward compatibility
    public RecommendedFriendUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture, int mutualFriendCount, int sharedActivitiesCount) {
        this(id, name, email, username, bio, profilePicture, mutualFriendCount, sharedActivitiesCount, UserRelationshipType.RECOMMENDED_FRIEND, null);
    }
    
    // Legacy constructor for backward compatibility
    public RecommendedFriendUserDTO(UUID id, String name, String email, String username, String bio, String profilePicture, int mutualFriendCount) {
        this(id, name, email, username, bio, profilePicture, mutualFriendCount, 0, UserRelationshipType.RECOMMENDED_FRIEND, null);
    }
}
