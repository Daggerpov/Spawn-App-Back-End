package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserDTO extends BaseUserDTO {
    List<UUID> friendUserIds;
    List<UUID> friendTagIds;

    public UserDTO(UUID id, List<UUID> friendUserIds, String username, String picture, String firstName, String lastName, String bio, List<UUID> friendTagIds, String email) {
        super(id, firstName, lastName, email, username, bio, picture);
        this.friendUserIds = friendUserIds;
        this.friendTagIds = friendTagIds;
    }
}