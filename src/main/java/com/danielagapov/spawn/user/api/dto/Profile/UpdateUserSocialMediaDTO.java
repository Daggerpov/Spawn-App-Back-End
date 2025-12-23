package com.danielagapov.spawn.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserSocialMediaDTO {
    private String whatsappNumber;
    private String instagramUsername;
} 