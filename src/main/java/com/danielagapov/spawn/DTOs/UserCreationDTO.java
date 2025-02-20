package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserCreationDTO implements Serializable, AbstractUserDTO {
    private UUID id;
    private String username;
    private byte[] profilePictureData; // Changed from String to byte[]
    private String firstName;
    private String lastName;
    private String bio;
    private String email;
}