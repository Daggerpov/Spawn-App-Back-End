package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Repositories.IActivityTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivityTypeService implements IActivityTypeService {
    private IActivityTypeRepository repository;
    private ILogger logger;

    @Override
    public List<ActivityType> getActivityTypesByUserId(UUID userId) {
        return repository.findActivityTypesByCreatorId(userId);
    }

    @Override
    public void updateActivityTypes(UUID userId, List<ActivityType> activityTypes) {
        try {
            repository.saveAll(activityTypes);
        } catch (Exception e) {
            if (!activityTypes.isEmpty()) {
                String username = activityTypes.get(0).getCreator().getUsername();
                logger.error("Error batch saving activity types for " + username + ". Error: " + e.getMessage());
            }
            logger.error("Error batch saving activity types. Error: " + e.getMessage());
        }
    }
}
