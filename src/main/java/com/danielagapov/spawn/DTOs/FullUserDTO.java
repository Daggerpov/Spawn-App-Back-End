package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullUserDTO extends AbstractUserDTO implements Serializable {
    List<FullUserDTO> friends;
    String username;
    String bio;
    List<FullFriendTagDTO> friendTags;
    String profilePicture;
    public FullUserDTO(UUID id, List<FullUserDTO> friends, String username, String picture, String firstName, String lastName, String bio, List<FullFriendTagDTO> friendTags, String email) {
        super(id, firstName, lastName, email);
        this.friends = friends;
        this.username = username;
        this.bio = bio;
        this.friendTags = friendTags;
        this.profilePicture = picture;
    }
}