package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.UUID;

public record FriendTagDTO(
        UUID id,
        String displayName,
        String color, // TODO: investigate data type later
        UserDTO owner
) implements Serializable {}