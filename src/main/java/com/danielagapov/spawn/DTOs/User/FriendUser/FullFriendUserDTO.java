package com.danielagapov.spawn.DTOs.User.FriendUser;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FullFriendUserDTO extends BaseUserDTO {

    public FullFriendUserDTO(UUID id, String username, String profilePicture, String name,
                             String bio, String email) {
        super(id, name, email, username, bio, profilePicture);
    }


}
