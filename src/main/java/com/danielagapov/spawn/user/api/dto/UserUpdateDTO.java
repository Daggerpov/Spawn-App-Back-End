package com.danielagapov.spawn.user.api.dto;

import com.danielagapov.spawn.shared.validation.ValidName;
import com.danielagapov.spawn.shared.validation.ValidUsername;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;
    
    @ValidUsername(optional = true)
    private String username;
    
    @ValidName(optional = true)
    private String name;
} 