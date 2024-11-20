package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;

public record UserDTO(
        Long id,
        String username,
        String firstName,
        String lastName,
        String bio,
        String profilePicture, // TODO: adjust data type later
        List<FriendTagDTO> friendTags
) implements Serializable {}