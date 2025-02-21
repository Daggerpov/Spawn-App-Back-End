package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class UserDTO extends AbstractUserDTO implements Serializable, IOnboardedUserDTO {
    List<UUID> friendIds;
    String username;
    String bio;
    List<UUID> friendTagIds;
}