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
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String bio;
}
