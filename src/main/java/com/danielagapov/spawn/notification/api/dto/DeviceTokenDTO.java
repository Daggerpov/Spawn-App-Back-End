package com.danielagapov.spawn.notification.api.dto;

import com.danielagapov.spawn.shared.util.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenDTO {
    private String token;
    private DeviceType deviceType;
    private UUID userId;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null check and class check
        DeviceTokenDTO that = (DeviceTokenDTO) obj; // Safe cast
        return token != null && token.equals(that.token); // Compare tokens
    }
    
    @Override
    public int hashCode() {
        return token != null ? token.hashCode() : 0;
    }
} 