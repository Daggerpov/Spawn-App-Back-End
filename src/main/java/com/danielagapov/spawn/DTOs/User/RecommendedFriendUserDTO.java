package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RecommendedFriendUserDTO extends FullUserDTO implements Serializable {
    int mutualFriendCount; // only added property from `FullUserDTO`

    public RecommendedFriendUserDTO(UUID id, List<UserDTO> friends, String username, String picture, String firstName, String lastName, String bio, List<FriendTagDTO> friendTags, String email, int mutualFriendCount) {
        super(id, friends, username, picture, firstName, lastName, bio, friendTags, email);
        this.mutualFriendCount = mutualFriendCount;
    }
}