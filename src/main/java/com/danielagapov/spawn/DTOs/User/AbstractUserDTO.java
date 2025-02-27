package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
// Abstract class since interface describes behaviours
public abstract class AbstractUserDTO implements Serializable {
    UUID id;
    String firstName;
    String lastName;
    String email;
    String username;
    String bio;
}
