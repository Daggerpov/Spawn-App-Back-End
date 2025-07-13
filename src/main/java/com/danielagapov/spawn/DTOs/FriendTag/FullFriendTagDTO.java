package com.danielagapov.spawn.DTOs.FriendTag;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * DTO for friend tags with full user information.
 * Similar to FriendTagDTO but includes complete BaseUserDTO objects instead of just user IDs.
 */
@Getter
@Setter
@NoArgsConstructor
public class FullFriendTagDTO extends AbstractFriendTagDTO {
    private List<BaseUserDTO> friends;
    @JsonProperty("isEveryone") // Explicitly define JSON property name
    private boolean isEveryone;

    public FullFriendTagDTO(UUID id, String displayName, String colorHexCode,
                           List<BaseUserDTO> friends, boolean isEveryone) {
        super(id, displayName, colorHexCode);
        this.friends = friends;
        this.isEveryone = isEveryone;
    }

    public List<BaseUserDTO> getFriends() {
        return friends;
    }

    public void setFriends(List<BaseUserDTO> friends) {
        this.friends = friends;
    }
} 