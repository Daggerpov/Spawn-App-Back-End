package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class SendEmailVerificationRequestDTO implements Serializable {
    private final String email;
} 