package com.danielagapov.spawn.DTOs;

import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Getter
public class SendVerificationRequestDTO implements Serializable {
    private final String phoneNumber;
    private final UUID userId;

    public SendVerificationRequestDTO(String phoneNumber, UUID userId) {
        this.phoneNumber = phoneNumber;
        this.userId = userId;
    }
}
