package com.danielagapov.spawn.Services.UserProfileInfo;

import com.danielagapov.spawn.DTOs.User.Profile.UserProfileInfoDTO;

import java.util.UUID;

public interface IUserProfileInfoService {
    /**
     * Get user profile information including creation date
     * 
     * @param userId User ID to get profile info for
     * @return UserProfileInfoDTO containing user profile information
     */
    UserProfileInfoDTO getUserProfileInfo(UUID userId);
} 