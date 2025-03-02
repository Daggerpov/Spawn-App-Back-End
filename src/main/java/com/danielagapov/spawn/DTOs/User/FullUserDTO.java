package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullUserDTO extends AbstractUserDTO implements Serializable {
    List<UserDTO> friends;
    List<FriendTagDTO> friendTags;
    String profilePicture;

    public FullUserDTO(UUID id, List<UserDTO> friends, String username, String picture, String firstName, String lastName, String bio, List<FriendTagDTO> friendTags, String email) {
        super(id, firstName, lastName, email, username, bio);
        this.friends = friends;
        this.friendTags = friendTags;
        this.profilePicture = picture;
    }
}