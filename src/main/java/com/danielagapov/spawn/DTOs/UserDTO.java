package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String bio,
        String profilePicture, // TODO: adjust data type later
        List<FriendTagDTO> friendTags
) implements Serializable {}