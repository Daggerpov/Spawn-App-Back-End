package com.danielagapov.spawn.DTOs;

import lombok.Getter;

@Getter
public class CheckVerificationRequestDTO {
    private final String phoneNumber;
    private final String verificationCode;

    public CheckVerificationRequestDTO(String phoneNumber, String verificationCode) {
        this.phoneNumber = phoneNumber;
        this.verificationCode = verificationCode;
    }
}
