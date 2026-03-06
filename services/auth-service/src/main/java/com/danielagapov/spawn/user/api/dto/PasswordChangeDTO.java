package com.danielagapov.spawn.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO used for password change requests
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeDTO {
    private String currentPassword;
    private String newPassword;
} 