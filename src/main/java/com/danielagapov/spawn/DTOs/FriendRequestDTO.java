package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.UUID;

public record FriendRequestDTO(UUID id, UserDTO sender, UserDTO receiver) implements Serializable {
}
