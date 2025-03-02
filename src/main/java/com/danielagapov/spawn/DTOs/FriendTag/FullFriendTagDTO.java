package com.danielagapov.spawn.DTOs.FriendTag;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFriendTagDTO extends AbstractFriendTagDTO implements Serializable {
    List<BaseUserDTO> friends;

    public FullFriendTagDTO(UUID id, String displayName, String colorHexCode,
                            List<BaseUserDTO> friends,
                            boolean isEveryone) {
        super(id, displayName, colorHexCode, isEveryone);
        this.friends = friends;
    }
}