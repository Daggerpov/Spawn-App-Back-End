package com.danielagapov.spawn.DTOs.User.Profile;

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