package com.danielagapov.spawn.DTOs.FriendTag;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFriendTagDTO extends AbstractFriendTagDTO implements Serializable {
    List<BaseUserDTO> friends;
    @JsonProperty("isEveryone") // Explicitly define JSON property name
    private boolean isEveryone;

    public FullFriendTagDTO(UUID id, String displayName, String colorHexCode,
                            List<BaseUserDTO> friends,
                            boolean isEveryone) {
        super(id, displayName, colorHexCode);
        this.friends = friends;
        this.isEveryone = isEveryone;
    }
}