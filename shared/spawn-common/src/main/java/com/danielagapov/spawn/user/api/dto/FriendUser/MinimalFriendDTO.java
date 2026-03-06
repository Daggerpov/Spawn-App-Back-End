package com.danielagapov.spawn.user.api.dto.FriendUser;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Minimal DTO for friend users (id, username, name, profilePicture).
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

    public static MinimalFriendDTO fromBaseUserDTO(BaseUserDTO baseUser) {
        return new MinimalFriendDTO(
            baseUser.getId(),
            baseUser.getUsername(),
            baseUser.getName(),
            baseUser.getProfilePicture()
        );
    }
}
