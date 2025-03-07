package com.danielagapov.spawn.DTOs.User.FriendUser;

import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public abstract class AbstractFriendUserDTO extends AbstractUserDTO implements Serializable {
    private String profilePicture;

    public AbstractFriendUserDTO(UUID id, String firstName, String lastName, String email, String username, String bio, String profilePicture) {
        super(id, firstName, lastName, email, username, bio);
        this.profilePicture = profilePicture;
    }
}
