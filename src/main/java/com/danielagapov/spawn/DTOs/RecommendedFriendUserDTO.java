package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record RecommendedFriendUserDTO(
    UUID id,
    List<UserDTO> friends,
    String username,
    String profilePicture, // TODO: adjust data type later
    String firstName,
    String lastName,
    String bio,
    List<FriendTagDTO> friendTags,
    String email,
    // Added property from `FullUserDTO`:
    int mutualFriendCount
) implements Serializable, IOnboardedUserDTO {}
