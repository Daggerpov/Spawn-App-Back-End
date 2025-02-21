package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RecommendedFriendUserDTO extends FullUserDTO implements Serializable, IOnboardedUserDTO {
    int mutualFriendCount;
    public RecommendedFriendUserDTO(UUID id, List<FullUserDTO> friends, String username, String picture, String firstName, String lastName, String bio, List<FullFriendTagDTO> friendTags, String email, int mutualFriendCount) {
        super(id, friends, username, picture, firstName, lastName, bio, friendTags, email);
        this.mutualFriendCount = mutualFriendCount;
    }
}
