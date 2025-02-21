package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
// Abstract class since interface describes behaviours
public abstract class AbstractUserDTO implements IOnboardedUserDTO{
    UUID id;
    String firstName;
    String lastName;
    String email;
    String profilePicture;
}
