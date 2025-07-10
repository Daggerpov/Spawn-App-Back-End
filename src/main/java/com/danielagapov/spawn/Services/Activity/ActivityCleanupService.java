package com.danielagapov.spawn.Services.Activity;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Repositories.IActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class ActivityCleanupService {
    private static final long RATE = 1000 * 60 * 60; // 1 hour
    private final ILogger logger;
    private final IActivityRepository activityRepository;

    @Autowired
    @Lazy
    public ActivityCleanupService(ILogger logger, IActivityRepository activityRepository) {
        this.logger = logger;
        this.activityRepository = activityRepository;
    }

    /**
     * This method is scheduled to be invoked every RATE ms (currently RATE = 1 hour).
     * It is used to delete indefinite activities that have passed their expiry time.
     * An indefinite activity expires at midnight of the day it was created.
     */
    @Scheduled(fixedRate = RATE)
    public void cleanExpiredIndefiniteActivities() {
        logger.info("Cleaning expired indefinite activities");
        try {
            OffsetDateTime now = OffsetDateTime.now();
            
            // Calculate the cutoff time - midnight of today
            // Activities created before today's midnight should be removed
            OffsetDateTime cutoffTime = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            Instant cutoffInstant = cutoffTime.toInstant();
            
            int numDeleted = activityRepository.deleteExpiredIndefiniteActivities(cutoffInstant);
            logger.info(String.format("Successfully deleted %d expired indefinite activities from database", numDeleted));
        } catch (Exception e) {
            logger.error("Unexpected error while deleting expired indefinite activities: " + e.getMessage());
        }
    }
} 