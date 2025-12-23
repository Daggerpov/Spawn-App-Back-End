package com.danielagapov.spawn.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileInfoDTO implements Serializable {
    private UUID userId;
    private String name;
    private String username;
    private String bio;
    private String profilePicture;
    private Date dateCreated;
} 