package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.UUID;

public record FriendRequestDTO(UUID id, UUID senderUserId, UUID receiverUserId) implements Serializable {
}
