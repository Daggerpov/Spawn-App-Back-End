package com.danielagapov.spawn.user.internal.services;

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

    /**
     * Remove an interest for a user
     * @param userId The ID of the user
     * @param encodedInterestName The URL-encoded name of the interest to remove
     * @return true if the interest was successfully removed, false if not found
     */
    boolean removeUserInterest(UUID userId, String encodedInterestName);
} 