package com.danielagapov.spawn.DTOs.User.Profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSocialMediaDTO {
    private UUID id;
    private UUID userId;
    private String whatsappLink;
    private String instagramLink;
} 