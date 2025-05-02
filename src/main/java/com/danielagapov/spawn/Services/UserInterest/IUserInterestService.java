package com.danielagapov.spawn.Services.UserInterest;

import com.danielagapov.spawn.DTOs.User.Profile.CreateUserInterestDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserInterestDTO;

import java.util.List;
import java.util.UUID;

public interface IUserInterestService {
    /**
     * Get all interests for a user
     * @param userId The ID of the user
     * @return A list of user interests
     */
    List<UserInterestDTO> getUserInterests(UUID userId);

    /**
     * Add a new interest for a user
     * @param createUserInterestDTO DTO containing the user ID and interest
     * @return The created user interest
     */
    UserInterestDTO addUserInterest(CreateUserInterestDTO createUserInterestDTO);
} 