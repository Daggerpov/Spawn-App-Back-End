package com.danielagapov.spawn.DTOs;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class SendVerificationRequestDTO implements Serializable {
    private final String phoneNumber;

    public SendVerificationRequestDTO(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
