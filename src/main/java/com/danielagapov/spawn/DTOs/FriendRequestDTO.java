package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Models.User;

import java.io.Serializable;
import java.util.UUID;

public record FriendRequestDTO(UUID id, User sender, User receiver) implements Serializable {
}
