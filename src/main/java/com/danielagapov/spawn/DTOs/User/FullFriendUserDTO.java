package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.DTOs.FriendTag.FullFriendTagDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFriendUserDTO extends FullUserDTO implements Serializable {
    private List<FullFriendTagDTO> associatedFriendTagsToOwner; // only added property from `FullUserDTO`

    public FullFriendUserDTO(UUID id, List<FullUserDTO> friends, String username, String profilePicture, String firstName,
                             String lastName, String bio, List<FullFriendTagDTO> friendTags, String email, List<FullFriendTagDTO> associatedFriendTagsToOwner) {
        super(id, friends, username, profilePicture, firstName, lastName, bio, friendTags, email);
        this.associatedFriendTagsToOwner = associatedFriendTagsToOwner;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof FullFriendUserDTO other))
            return false;
        boolean superEquals = super.equals(other);
        return other.getAssociatedFriendTagsToOwner().equals(this.associatedFriendTagsToOwner) && superEquals;
    }
}
