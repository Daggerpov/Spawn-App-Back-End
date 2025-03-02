package com.danielagapov.spawn.DTOs.User.FriendUser;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PotentialFriendUserDTO extends AbstractFriendUserDTO {
    public PotentialFriendUserDTO(UUID id, String firstName, String lastName, String email, String username, String bio, String profilePicture) {
        super(id, firstName, lastName, email, username, bio, profilePicture);
    }
    
}
