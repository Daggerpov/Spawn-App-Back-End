package com.danielagapov.spawn.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CheckEmailVerificationRequestDTO {
    @JsonProperty("email")
    private final String email;
    @JsonProperty("verificationCode")
    private final String verificationCode;

    public CheckEmailVerificationRequestDTO(String email, String verificationCode) {
        this.email = email;
        this.verificationCode = verificationCode;
    }
} 