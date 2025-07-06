package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class LoginDTO implements Serializable {
    final String username; // Could be an email
    final String password;
}
