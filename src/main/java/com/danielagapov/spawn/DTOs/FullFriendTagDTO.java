package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFriendTagDTO extends AbstractFriendTagDTO implements Serializable {
    List<UserDTO> friends;
    public FullFriendTagDTO(UUID id, String displayName, String colorHexCode,
                            List<UserDTO> friends,
                            boolean isEveryone) {
        super(id, displayName, colorHexCode, isEveryone);
        this.friends = friends;
    }
}