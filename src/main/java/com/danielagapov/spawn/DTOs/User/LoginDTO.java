package com.danielagapov.spawn.DTOs.User;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor(onConstructor = @__(@JsonCreator))
@Getter
public class LoginDTO implements Serializable {
    @JsonProperty("usernameOrEmail")
    private final String usernameOrEmail;
    @JsonProperty("password")
    private final String password;
}
