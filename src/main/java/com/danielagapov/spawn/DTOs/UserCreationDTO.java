package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class UserCreationDTO extends AbstractUserDTO implements Serializable {
    private String username;
    private byte[] profilePictureData; // Changed from String to byte[]
    private String bio;
    public UserCreationDTO( UUID id, String username, byte[] profilePictureData, String firstName, String lastName, String bio, String email) {
        // TODO investigate if its ok to leave the superclass with an empty profilePicture string
        super(id, firstName, lastName, email, "");
        this.profilePictureData = profilePictureData;
        this.username = username;
        this.bio = bio;
    }
}