package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class RecommendedFriendUserDTO extends AbstractFriendUserDTO implements Serializable {
    int mutualFriendCount;


    public RecommendedFriendUserDTO(UUID id, String firstName, String lastName, String email, String username, String bio, String profilePicture, int mutualFriendCount) {
        super(id, firstName, lastName, email, username, bio, profilePicture);
        this.mutualFriendCount = mutualFriendCount;
    }
}