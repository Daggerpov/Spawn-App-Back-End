package com.danielagapov.spawn.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SendEmailVerificationRequestDTO implements Serializable {
    private String email;
} 