package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LoginDTO implements Serializable {
    private String usernameOrEmail;
    private String password;
}
