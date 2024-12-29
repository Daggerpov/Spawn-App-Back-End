package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record FriendTagDTO(
        UUID id,
        String displayName,
        String colorHexCode, // TODO: investigate data type later
        UUID ownerId,
        List<UUID> friends
) implements Serializable {}