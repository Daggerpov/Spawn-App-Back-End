package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.DTOs.FriendTag.FullFriendTagDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullUserDTO extends AbstractUserDTO implements Serializable {
    List<FullUserDTO> friends;
    List<FullFriendTagDTO> friendTags;
    String profilePicture;

    public FullUserDTO(UUID id, List<FullUserDTO> friends, String username, String picture, String firstName, String lastName, String bio, List<FullFriendTagDTO> friendTags, String email) {
        super(id, firstName, lastName, email, username, bio);
        this.friends = friends;
        this.friendTags = friendTags;
        this.profilePicture = picture;
    }
}