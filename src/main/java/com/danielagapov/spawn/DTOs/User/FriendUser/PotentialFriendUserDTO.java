package com.danielagapov.spawn.DTOs.User.FriendUser;

import com.danielagapov.spawn.Models.User;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PotentialFriendUserDTO extends AbstractFriendUserDTO {
    public PotentialFriendUserDTO(UUID id, String firstName, String lastName, String email, String username, String bio, String profilePicture) {
        super(id, firstName, lastName, email, username, bio, profilePicture);
    }

    public static PotentialFriendUserDTO fromUserEntity(User user) {
        return new PotentialFriendUserDTO(
                user.getId(),
                user.getFirstName(),
                user.getBio(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString()
        );
    }
}
