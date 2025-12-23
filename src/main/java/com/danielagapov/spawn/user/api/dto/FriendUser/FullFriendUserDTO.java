package com.danielagapov.spawn.user.api.dto;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
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
