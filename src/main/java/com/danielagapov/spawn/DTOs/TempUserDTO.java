package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class TempUserDTO extends AbstractUserDTO implements Serializable {
    public TempUserDTO(UUID id, String firstName, String lastName, String email, String picture) {
        super.id = id;
        super.firstName = firstName;
        super.lastName = lastName;
        super.email = email;
        super.profilePicture = picture;
    }
}
