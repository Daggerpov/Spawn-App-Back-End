package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record FriendTagDTO(
        UUID id,
        String displayName,
        String colorHexCode, // TODO: investigate data type later
        UUID owner, // Will also cause circular DTO mapping
        List<UUID> friends // If not UUID this will cause circular DTO mapping (stack overflow)
) implements Serializable {}