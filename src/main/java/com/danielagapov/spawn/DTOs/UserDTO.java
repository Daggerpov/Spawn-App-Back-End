package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record UserDTO(
        UUID id,
        List<UUID> friendIds,
        String username,
        String profilePicture, // TODO: adjust data type later
        String firstName,
        String lastName,
        String bio,
        List<UUID> friendTagIds,
        String email
) implements Serializable, AbstractUserDTO {}