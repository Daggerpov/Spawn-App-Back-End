package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullUserDTO extends BaseUserDTO {
    private List<UserDTO> friends;
    private List<FriendTagDTO> friendTags;

    public FullUserDTO(UUID id, List<UserDTO> friends, String username, String picture, String firstName, String lastName, String bio, List<FriendTagDTO> friendTags, String email) {
        super(id, firstName, lastName, email, username, bio, picture);
        this.friends = friends;
        this.friendTags = friendTags;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof FullUserDTO other))
            return false;
        boolean fullUserTraitsEqual = other.getFriends().equals(this.friends) &&
                                        other.getFriendTags().equals(this.friendTags) &&
                                        other.getProfilePicture().equals(this.profilePicture);
        boolean superEquals = super.equals(other);
        return fullUserTraitsEqual && superEquals;
    }
}