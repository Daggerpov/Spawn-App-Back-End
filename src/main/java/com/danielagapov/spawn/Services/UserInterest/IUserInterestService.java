package com.danielagapov.spawn.Services.UserInterest;

import com.danielagapov.spawn.DTOs.User.Profile.UserInterestDTO;

import java.util.List;
import java.util.UUID;

public interface IUserInterestService {
    /**
     * Get all interests for a user
     * @param userId The ID of the user
     * @return A list of user interest strings
     */
    List<String> getUserInterests(UUID userId);

    /**
     * Add a new interest for a user
     * @param user id and interest name
     * @return The created user interest string
     */
    String addUserInterest(UUID userId, String interestName);
} 