package com.danielagapov.spawn.DTOs.User.FriendUser;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFriendUserDTO extends BaseUserDTO {
    private List<FriendTagDTO> associatedFriendTagsToOwner;

    public FullFriendUserDTO(UUID id, String username, String profilePicture, String firstName,
                             String lastName, String bio, String email, List<FriendTagDTO> associatedFriendTagsToOwner) {
        super(id, firstName, lastName, email, username, bio, profilePicture);
        this.associatedFriendTagsToOwner = associatedFriendTagsToOwner;
    }
}
