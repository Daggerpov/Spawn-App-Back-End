package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFriendUserDTO extends FullUserDTO implements Serializable {
    private List<FriendTagDTO> associatedFriendTagsToOwner; // only added property from `FullUserDTO`

    public FullFriendUserDTO(UUID id, List<UserDTO> friends, String username, String profilePicture, String firstName,
                             String lastName, String bio, List<FriendTagDTO> friendTags, String email, List<FriendTagDTO> associatedFriendTagsToOwner) {
        super(id, friends, username, profilePicture, firstName, lastName, bio, friendTags, email);
        this.associatedFriendTagsToOwner = associatedFriendTagsToOwner;
    }
}
