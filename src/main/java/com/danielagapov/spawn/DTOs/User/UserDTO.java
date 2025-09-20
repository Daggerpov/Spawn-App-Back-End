package com.danielagapov.spawn.DTOs.User;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonCreator
    public UserDTO(
            @JsonProperty("id") UUID id, 
            @JsonProperty("friendUserIds") List<UUID> friendUserIds, 
            @JsonProperty("username") String username, 
            @JsonProperty("profilePicture") String picture, 
            @JsonProperty("name") String name, 
            @JsonProperty("bio") String bio, 
            @JsonProperty("email") String email, 
            @JsonProperty("hasCompletedOnboarding") Boolean hasCompletedOnboarding) {
        super(id, name, email, username, bio, picture, hasCompletedOnboarding);
        this.friendUserIds = friendUserIds;
    }
}