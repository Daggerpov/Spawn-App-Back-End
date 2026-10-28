package com.danielagapov.spawn.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationResponseDTO implements Serializable {
    private long secondsUntilNextAttempt;
    private String message;
} 