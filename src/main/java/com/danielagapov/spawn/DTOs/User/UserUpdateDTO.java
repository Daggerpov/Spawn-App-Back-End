package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {
    private String bio;
    private String username;
    private String firstName;
    private String lastName;
} 