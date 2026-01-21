package com.danielagapov.spawn.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * A minimal DTO for friend users, containing only the essential fields needed
 * for displaying friends in selection lists (e.g., activity creation, activity types).
 * 
 * This DTO significantly reduces memory usage compared to FullFriendUserDTO by
 * excluding fields like bio and email that are unnecessary for friend selection UIs.
 * 
 * Fields included:
 * - id: Required for selection/identification
 * - username: Displayed as @username
 * - name: Displayed as the friend's name
 * - profilePicture: URL for avatar display
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MinimalFriendDTO implements Serializable {
    private UUID id;
    private String username;
    private String name;
    private String profilePicture;
    
    /**
     * Creates a MinimalFriendDTO from a FullFriendUserDTO
     */
    public static MinimalFriendDTO fromFullFriendUserDTO(FullFriendUserDTO fullFriend) {
        return new MinimalFriendDTO(
            fullFriend.getId(),
            fullFriend.getUsername(),
            fullFriend.getName(),
            fullFriend.getProfilePicture()
        );
    }
    
    /**
     * Creates a MinimalFriendDTO from a BaseUserDTO
     */
    public static MinimalFriendDTO fromBaseUserDTO(BaseUserDTO baseUser) {
        return new MinimalFriendDTO(
            baseUser.getId(),
            baseUser.getUsername(),
            baseUser.getName(),
            baseUser.getProfilePicture()
        );
    }
}
