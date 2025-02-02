package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record FullUserDTO(
        UUID id,
        List<FullUserDTO> friends,
        String username,
        String profilePicture,
        String firstName,
        String lastName,
        String bio,
        List<FullFriendTagDTO> friendTags,
        String email
) implements Serializable, IOnboardedUserDTO {}