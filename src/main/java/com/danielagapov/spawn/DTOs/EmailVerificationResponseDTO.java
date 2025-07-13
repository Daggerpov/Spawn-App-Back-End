package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class EmailVerificationResponseDTO implements Serializable {
    private final long secondsUntilNextAttempt;
    private final String message;
} 