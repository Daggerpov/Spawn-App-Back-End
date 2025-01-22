package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record FullFriendTagDTO(
        UUID id,
        String displayName,
        String colorHexCode,
        UserDTO ownerUser,
        List<UserDTO> friendUsers,
        boolean isEveryone
) implements Serializable, IFriendTagDTO {}