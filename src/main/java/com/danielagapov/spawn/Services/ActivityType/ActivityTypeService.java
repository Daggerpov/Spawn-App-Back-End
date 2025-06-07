package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ActivityTypeMapper;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityTypeRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivityTypeService implements IActivityTypeService {
    private IActivityTypeRepository repository;
    private ILogger logger;
    private IUserService userService;

    @Override
    public List<ActivityType> getActivityTypesByUserId(UUID userId) {
        return repository.findActivityTypesByCreatorId(userId);
    }

    @Override
    public void updateActivityTypes(UUID userId, List<ActivityTypeDTO> activityTypeDTOs) {
        try {
            User creator = userService.getUserEntityById(userId);
            List<ActivityType> activityTypes = ActivityTypeMapper.toEntityList(activityTypeDTOs, creator);
            repository.saveAll(activityTypes);
        } catch (Exception e) {
            logger.error("Error batch saving activity types. Error: " + e.getMessage());
        }
    }
}
