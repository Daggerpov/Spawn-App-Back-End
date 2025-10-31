package com.danielagapov.spawn.Services.UserInterest;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.User.Profile.UserInterest;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Repositories.User.Profile.UserInterestRepository;
import com.danielagapov.spawn.Util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public final class UserInterestService implements IUserInterestService {

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
        List<UserInterest> interests = userInterestRepository.findByUserId(userId);
        return interests.stream()
                .map(UserInterest::getInterest)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "userInterests", key = "#userId")
    public String addUserInterest(UUID userId, String interestName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserInterest userInterest = new UserInterest(user, interestName);
        userInterest = userInterestRepository.save(userInterest);

        return userInterest.getInterest();
    }

    @Override
    @CacheEvict(value = "userInterests", key = "#userId")
    public boolean removeUserInterest(UUID userId, String encodedInterestName) {
        try {
            // URL decode the interest name to handle spaces and special characters
            String decodedInterest = URLDecoder.decode(encodedInterestName, StandardCharsets.UTF_8);
            
            logger.info("Attempting to remove interest '" + decodedInterest + "' (encoded: '" + encodedInterestName + "') for user: " + LoggingUtils.formatUserIdInfo(userId));
            
            // Debug: Log all existing interests for this user
            List<UserInterest> allUserInterests = userInterestRepository.findByUserId(userId);
            logger.info("User " + LoggingUtils.formatUserIdInfo(userId) + " currently has " + allUserInterests.size() + " interests:");
            for (UserInterest existingInterest : allUserInterests) {
                logger.info("  - '" + existingInterest.getInterest() + "' (length: " + existingInterest.getInterest().length() + ")");
            }
            
            Optional<UserInterest> userInterestOpt = userInterestRepository.findByUserIdAndInterest(userId, decodedInterest);
            
            if (userInterestOpt.isPresent()) {
                userInterestRepository.delete(userInterestOpt.get());
                logger.info("Successfully removed interest '" + decodedInterest + "' for user: " + LoggingUtils.formatUserIdInfo(userId));
                return true;
            } else {
                logger.warn("Interest '" + decodedInterest + "' not found for user: " + LoggingUtils.formatUserIdInfo(userId));
                logger.warn("Exact search failed. Trying case-insensitive search...");
                
                // Try case-insensitive search for debugging
                for (UserInterest existingInterest : allUserInterests) {
                    if (existingInterest.getInterest().equalsIgnoreCase(decodedInterest)) {
                        logger.warn("Found case-insensitive match: '" + existingInterest.getInterest() + "' vs '" + decodedInterest + "'");
                        break;
                    }
                }
                
                return false;
            }
        } catch (Exception e) {
            logger.error("Error removing interest '" + encodedInterestName + "' for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw new RuntimeException("Failed to remove user interest", e);
        }
    }
} 