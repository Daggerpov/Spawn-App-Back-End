package com.danielagapov.spawn.DTOs;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class SendEmailVerificationRequestDTO implements Serializable {
    private final String email;

    public SendEmailVerificationRequestDTO(String email) {
        this.email = email;
    }
} 