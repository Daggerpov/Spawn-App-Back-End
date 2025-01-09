package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record FriendTagDTO(
        UUID id,
        String displayName,
        String colorHexCode,
        UserDTO owner,
        List<UserDTO> friends,
        Boolean isEveryone
) implements Serializable {}