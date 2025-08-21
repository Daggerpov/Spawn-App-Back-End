package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.Enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO implements Serializable {
    private BaseUserDTO user;
    private UserStatus status;
    private Boolean isOAuthUser;

    public AuthResponseDTO(BaseUserDTO user, UserStatus status) {
        this.user = user;
        this.status = status;
    }
} 