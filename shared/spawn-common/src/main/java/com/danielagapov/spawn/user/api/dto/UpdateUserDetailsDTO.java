package com.danielagapov.spawn.user.api.dto;

import com.danielagapov.spawn.shared.validation.ValidPhoneNumber;
import com.danielagapov.spawn.shared.validation.ValidUsername;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDetailsDTO implements Serializable {
    @NotNull(message = "User ID is required")
    private UUID id;
    
    @ValidUsername
    private String username;
    
    @ValidPhoneNumber
    private String phoneNumber;
    
    private String password;
} 