package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDetailsDTO implements Serializable {
    private UUID id;
    private String username;
    private String phoneNumber;
    private String password; // Optional, can be null if not updating password
} 