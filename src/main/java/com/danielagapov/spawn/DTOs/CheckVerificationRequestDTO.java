package com.danielagapov.spawn.DTOs;

import lombok.Getter;

import java.util.UUID;

@Getter
public class CheckVerificationRequestDTO {
    private final String phoneNumber;
    private final String verificationCode;
    private final UUID userId;

    public CheckVerificationRequestDTO(String phoneNumber, String verificationCode, UUID userId) {
        this.phoneNumber = phoneNumber;
        this.verificationCode = verificationCode;
        this.userId = userId;
    }
}
