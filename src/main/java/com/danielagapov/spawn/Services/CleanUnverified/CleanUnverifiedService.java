package com.danielagapov.spawn.Services.CleanUnverified;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CleanUnverifiedService {
    private static final long RATE = 1000 * 60 * 60 * 24; // 24 hours
    private final ILogger logger;
    private final IUserRepository userRepository;

    @Autowired
    @Lazy
    public CleanUnverifiedService(ILogger logger, IUserRepository userRepository) {
        this.logger = logger;
        this.userRepository = userRepository;
    }

    /**
     * This method is scheduled to be invoked every RATE ms (currently RATE = 24 hours).
     * It is used to delete unverified users whose verification links have expired which is determined by the date
     * their account was created.
     * If a user is both unverified AND their account was created more than RATE ms ago, they will be deleted
     */
    @Scheduled(fixedRate = RATE)
    public void cleanUnverifiedExpiredUsers() {
        logger.info("Cleaning unverified, expired users");
        try {
            int numDeleted = userRepository.deleteAllExpiredUnverifiedUsers();
            logger.info(String.format("Successfully deleted %s users from database", numDeleted));
        } catch (Exception e) {
            logger.error("Unexpected error while deleting expired, unverified users: " + e.getMessage());
        }
    }
}
