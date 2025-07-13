package com.danielagapov.spawn.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class SendEmailVerificationRequestDTO implements Serializable {
    @JsonProperty("email")
    private final String email;
} 