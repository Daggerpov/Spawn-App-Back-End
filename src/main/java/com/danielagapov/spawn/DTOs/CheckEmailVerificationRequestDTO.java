package com.danielagapov.spawn.DTOs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class CheckEmailVerificationRequestDTO {
    @JsonProperty("email")
    private final String email;
    @JsonProperty("verificationCode")
    private final String verificationCode;
} 