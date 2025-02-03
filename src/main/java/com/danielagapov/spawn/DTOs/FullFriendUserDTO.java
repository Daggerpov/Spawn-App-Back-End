package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FullFriendUserDTO implements Serializable, IOnboardedUserDTO {
    private UUID id;
    private List<FullUserDTO> friends;
    private String username;
    private String profilePicture;
    private String firstName;
    private String lastName;
    private String bio;
    private List<FullFriendTagDTO> friendTags;
    private String email;
    private List<FullFriendTagDTO> associatedFriendTagsToOwner; // only added property from `FullUserDTO`
}