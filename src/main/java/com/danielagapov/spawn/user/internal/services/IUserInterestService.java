package com.danielagapov.spawn.user.internal.services;

import java.util.List;
import java.util.UUID;

public interface IUserInterestService {
    List<String> getUserInterests(UUID userId);
    String addUserInterest(UUID userId, String interestName);
    boolean removeUserInterest(UUID userId, String interestName);
    List<String> replaceUserInterests(UUID userId, List<String> interests);
} 