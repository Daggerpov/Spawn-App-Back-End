package com.danielagapov.spawn.DTOs;

import lombok.Getter;

@Getter
public class CheckEmailVerificationRequestDTO {
    private final String email;
    private final String verificationCode;

    public CheckEmailVerificationRequestDTO(String email, String verificationCode) {
        this.email = email;
        this.verificationCode = verificationCode;
    }
} 