package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
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
    public List<ActivityTypeDTO> getActivityTypesByUserId(UUID userId) {
        return ActivityTypeMapper.toDTOList(repository.findActivityTypesByCreatorId(userId));
    }

    @Override
    public List<UUID> getPinnedActivityTypeIds(UUID userId) {
        // TODO: Implement pinned activity types functionality
        // This will require additional repository methods and possibly a UserActivityTypePin entity
        return List.of();
    }

    @Override
    public void toggleActivityTypePin(UUID userId, UUID activityTypeId, Boolean isPinned) {
        // TODO: Implement pinning functionality
        // This will require additional repository methods and possibly a UserActivityTypePin entity
        logger.info("Toggling pin status for activity type " + activityTypeId + " to " + isPinned + " for user: " + userId);
    }

    @Override
    public ActivityTypeDTO createActivityType(ActivityTypeDTO activityTypeDTO) {
        try {
            // For now, we'll assume the creator ID comes from the userId parameter in the controller
            // This method signature might need to be updated to include userId parameter
            logger.info("Creating activity type: " + activityTypeDTO.getTitle());
            // TODO: Implement proper creation logic with user ID
            return activityTypeDTO;
        } catch (Exception e) {
            logger.error("Error creating activity type: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public BatchActivityTypeUpdateDTO updateActivityTypes(UUID userId, BatchActivityTypeUpdateDTO activityTypeDTOs) {
        try {
            User creator = userService.getUserEntityById(userId);
            if (!activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                logger.info("Deleting activity types for user: " + creator.getUsername());
                repository.deleteAllById(activityTypeDTOs.getDeletedActivityTypeIds());
            }
            if (!activityTypeDTOs.getUpdatedActivityTypes().isEmpty()) {
                logger.info("Saving updated or newly created activity types for user: " + creator.getUsername());
                List<ActivityType> activityTypes = ActivityTypeMapper.toEntityList(activityTypeDTOs.getUpdatedActivityTypes(), creator);
                repository.saveAll(activityTypes);
            }
            return activityTypeDTOs;
        } catch (Exception e) {
            logger.error("Error batch saving activity types. Error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void initializeDefaultActivityTypesForUser(User user) {
        repository.save(new ActivityType(user, "Chill","🛋️"));
        repository.save(new ActivityType(user, "Food", "🍽️"));
        repository.save(new ActivityType(user, "Active", "🏃"));
        repository.save(new ActivityType(user, "Study", "✏️"));
    }

    @Override
    public void setOrderNumber(ActivityType activityType) {
        Integer maxOrder = repository.findMaxOrderNumberByCreatorId(activityType.getCreator().getId());
        activityType.setOrderNum(maxOrder != null ? maxOrder + 1 : 0);
    }

    @Override
    public void deleteActivityType(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.ActivityType, id);
        }

        try {
            repository.deleteById(id);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
