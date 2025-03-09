package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.DeviceType;
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
} 