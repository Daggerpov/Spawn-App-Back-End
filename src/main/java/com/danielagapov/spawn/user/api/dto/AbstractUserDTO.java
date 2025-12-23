package com.danielagapov.spawn.user.api.dto;

import com.danielagapov.spawn.shared.validation.ValidName;
import com.danielagapov.spawn.shared.validation.ValidUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
// Abstract class since interface describes behaviours
public abstract class AbstractUserDTO implements Serializable {
    private UUID id;
    
    @ValidName(optional = true)
    private String name;
    
    @Email(message = "Email must be valid")
    private String email;
    
    @ValidUsername(optional = true)
    private String username;
    
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null check and class check
        AbstractUserDTO that = (AbstractUserDTO) obj; // Safe cast
        return id != null && id.equals(that.id); // Compare IDs
    }
}
