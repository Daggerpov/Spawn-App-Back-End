package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenDTO {
    private String token;
    private DeviceType deviceType;
} 