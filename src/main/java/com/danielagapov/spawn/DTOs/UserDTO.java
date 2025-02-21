package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserDTO extends AbstractUserDTO implements Serializable {
    List<UUID> friendIds;
    String username;
    String bio;
    List<UUID> friendTagIds;
    String profilePicture;

    public UserDTO(UUID id, List<UUID> friendIds,String username, String picture, String firstName, String lastName, String bio, List<UUID> friendTagIds, String email) {
        super(id, firstName, lastName, email);
        this.friendIds = friendIds;
        this.username = username;
        this.bio = bio;
        this.friendTagIds = friendTagIds;
        this.profilePicture = picture;
    }
}