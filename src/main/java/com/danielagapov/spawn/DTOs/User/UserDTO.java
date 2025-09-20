package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserDTO extends BaseUserDTO {
    List<UUID> friendUserIds;

    public UserDTO(UUID id, List<UUID> friendUserIds, String username, String picture, String name, String bio, String email) {
        super(id, name, email, username, bio, picture);
        this.friendUserIds = friendUserIds;
    }

    public UserDTO(UUID id, List<UUID> friendUserIds, String username, String picture, String name, String bio, String email, Boolean hasCompletedOnboarding) {
        super(id, name, email, username, bio, picture, hasCompletedOnboarding);
        this.friendUserIds = friendUserIds;
    }
}