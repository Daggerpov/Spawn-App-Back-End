package com.danielagapov.spawn.DTOs.User;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class LoginDTO implements Serializable {
    @JsonProperty("usernameOrEmail")
    private final String usernameOrEmail;
    @JsonProperty("password")
    private final String password;
}
