package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class FullUserDTO extends AbstractUserDTO implements Serializable, IOnboardedUserDTO {
    List<FullUserDTO> friends;
    String username;
    String bio;
    List<FullFriendTagDTO> friendTags;
    public FullUserDTO(UUID id, List<FullUserDTO> friends, String username, String picture, String firstName, String lastName, String bio, List<FullFriendTagDTO> friendTags, String email) {
        super.id = id;
        this.friends = friends;
        this.username = username;
        super.profilePicture = picture;
        super.firstName = firstName;
        super.lastName = lastName;
        this.bio = bio;
        this.friendTags = friendTags;
        super.email = email;
    }
}