package com.danielagapov.spawn.DTOs;

import java.io.Serializable;

public record FriendTagDTO(
        Long id,
        String displayName,
        String color, // TODO: investigate data type later
        UserDTO owner
) implements Serializable {}