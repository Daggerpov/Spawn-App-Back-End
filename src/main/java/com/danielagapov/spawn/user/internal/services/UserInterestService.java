package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.domain.UserInterest;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.user.internal.repositories.UserInterestRepository;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserInterestService implements IUserInterestService {

    private final UserInterestRepository userInterestRepository;
    private final IUserRepository userRepository;
    private final ILogger logger;

    @Autowired
    public UserInterestService(UserInterestRepository userInterestRepository, IUserRepository userRepository, ILogger logger) {
        this.userInterestRepository = userInterestRepository;
        this.userRepository = userRepository;
        this.logger = logger;
    }

    @Override
    @Cacheable(value = "userInterests", key = "#userId")
    public List<String> getUserInterests(UUID userId) {
        return userInterestRepository.findByUserId(userId).stream()
                .map(UserInterest::getInterest)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "userInterests", key = "#userId")
    public String addUserInterest(UUID userId, String interestName) {
        String trimmed = interestName.trim();

        Optional<UserInterest> existing = userInterestRepository.findByUserIdAndInterestIgnoreCase(userId, trimmed);
        if (existing.isPresent()) {
            return existing.get().getInterest();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserInterest userInterest = new UserInterest(user, trimmed);
        userInterest = userInterestRepository.save(userInterest);
        return userInterest.getInterest();
    }

    @Override
    @CacheEvict(value = "userInterests", key = "#userId")
    public boolean removeUserInterest(UUID userId, String interestName) {
        // Spring already URL-decodes @PathVariable, so no manual decoding needed.
        // Use case-insensitive lookup to be resilient to casing mismatches.
        Optional<UserInterest> userInterestOpt = userInterestRepository.findByUserIdAndInterestIgnoreCase(userId, interestName);

        if (userInterestOpt.isPresent()) {
            userInterestRepository.delete(userInterestOpt.get());
            return true;
        }

        logger.warn("Interest '" + interestName + "' not found for user: " + LoggingUtils.formatUserIdInfo(userId));
        return false;
    }

    @Override
    @Transactional
    @CacheEvict(value = "userInterests", key = "#userId")
    public List<String> replaceUserInterests(UUID userId, List<String> interests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        userInterestRepository.deleteAllByUserId(userId);

        List<String> saved = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String interest : interests) {
            String trimmed = interest.trim();
            if (!trimmed.isEmpty() && seen.add(trimmed.toLowerCase())) {
                UserInterest entity = new UserInterest(user, trimmed);
                userInterestRepository.save(entity);
                saved.add(trimmed);
            }
        }
        return saved;
    }
} 