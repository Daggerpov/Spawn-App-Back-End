package com.danielagapov.spawn.DTOs.User;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class UpdateUserDetailsDTO implements Serializable {
    private UUID id;
    private String username;
    private String phoneNumber;
    private String password; // Optional, can be null if not updating password
} 