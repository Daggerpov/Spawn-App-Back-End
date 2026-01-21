package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.Profile.UpdateUserSocialMediaDTO;
import com.danielagapov.spawn.user.api.dto.Profile.UserSocialMediaDTO;

import java.util.UUID;

public interface IUserSocialMediaService {
    /**
     * Get social media links for a user
     * @param userId The ID of the user
     * @return DTO containing formatted social media links
     */
    UserSocialMediaDTO getUserSocialMedia(UUID userId);

    /**
     * Update social media information for a user
     * @param userId The ID of the user
     * @param updateDTO DTO containing social media details to update
     * @return DTO containing updated, formatted social media links
     */
    UserSocialMediaDTO updateUserSocialMedia(UUID userId, UpdateUserSocialMediaDTO updateDTO);
} 