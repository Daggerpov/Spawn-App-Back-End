package com.danielagapov.spawn.DTOs.User.FriendUser;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFriendUserDTO extends AbstractFriendUserDTO implements Serializable {
    private List<FriendTagDTO> associatedFriendTagsToOwner;

    public FullFriendUserDTO(UUID id, String username, String profilePicture, String firstName,
                             String lastName, String bio, String email, List<FriendTagDTO> associatedFriendTagsToOwner) {
        super(id, firstName, lastName, email, username, bio, profilePicture);
        this.associatedFriendTagsToOwner = associatedFriendTagsToOwner;
    }
}
