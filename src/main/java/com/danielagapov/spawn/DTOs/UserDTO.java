package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class UserDTO extends AbstractUserDTO implements Serializable, IOnboardedUserDTO {
    List<UUID> friendIds;
    String username;
    String bio;
    List<UUID> friendTagIds;

    public UserDTO(UUID id, List<UUID> friendIds,String username, String picture, String firstName, String lastName, String bio, List<UUID> friendTagIds, String email) {
        super.id = id;
        this.friendIds = friendIds;
        this.username = username;
        super.profilePicture = picture;
        super.firstName = firstName;
        super.lastName = lastName;
        this.bio = bio;
        this.friendTagIds = friendTagIds;
        super.email = email;
    }
}