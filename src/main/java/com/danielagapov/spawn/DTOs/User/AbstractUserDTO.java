package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
// Abstract class since interface describes behaviours
public abstract class AbstractUserDTO implements Serializable {
    private UUID id;
    private String name;
    private String email;
    private String username;
    private String bio;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null check and class check
        AbstractUserDTO that = (AbstractUserDTO) obj; // Safe cast
        return id != null && id.equals(that.id); // Compare IDs
    }
}
