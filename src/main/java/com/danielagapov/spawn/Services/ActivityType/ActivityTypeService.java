package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.ActivityTypeValidationException;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ActivityTypeService implements IActivityTypeService {
    private IActivityTypeRepository repository;
    private ILogger logger;
    private IUserService userService;

    // Constants for validation
    private static final int MAX_PINNED_ACTIVITY_TYPES = 3;

    @Override
    public List<ActivityTypeDTO> getActivityTypesByUserId(UUID userId) {
        // Returns activity types owned by this user
        return ActivityTypeMapper.toDTOList(repository.findActivityTypesByCreatorId(userId));
    }

    @Override
    public List<ActivityTypeDTO> updateActivityTypes(BatchActivityTypeUpdateDTO activityTypeDTOs) {
        try {
            if (activityTypeDTOs.getUpdatedActivityTypes().isEmpty() && activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                throw new IllegalArgumentException("No activity types to update or delete");
            }
            
            // Get userId for validation
            UUID userId = null;
            if (!activityTypeDTOs.getUpdatedActivityTypes().isEmpty()) {
                userId = activityTypeDTOs.getUpdatedActivityTypes().get(0).getOwnerUserId();
                
                // Validate the batch update
                validateActivityTypeUpdate(userId, activityTypeDTOs);
            }
            
            // Handle deletions first - no need to get userId for deletion
            if (!activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                logger.info("Deleting activity types with IDs: " + activityTypeDTOs.getDeletedActivityTypeIds());
                repository.deleteAllById(activityTypeDTOs.getDeletedActivityTypeIds());
            }
            
            // Handle updates/creations - need userId for these operations
            if (!activityTypeDTOs.getUpdatedActivityTypes().isEmpty()) {
                User creator = userService.getUserEntityById(userId);
                
                logger.info("Saving updated or newly created activity types for user: " + creator.getUsername());
                List<ActivityType> activityTypes = ActivityTypeMapper.toEntityList(activityTypeDTOs.getUpdatedActivityTypes(), creator);
                repository.saveAll(activityTypes);
            }
            
            // Return all activity types for the user after updates
            return getActivityTypesByUserId(userId != null ? userId : 
                activityTypeDTOs.getUpdatedActivityTypes().isEmpty() ? 
                null : activityTypeDTOs.getUpdatedActivityTypes().get(0).getOwnerUserId());
        } catch (Exception e) {
            logger.error("Error batch saving activity types. Error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<ActivityTypeDTO> updateActivityTypes(UUID userId, BatchActivityTypeUpdateDTO activityTypeDTOs) {
        try {
            if (activityTypeDTOs.getUpdatedActivityTypes().isEmpty() && activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                throw new IllegalArgumentException("No activity types to update or delete");
            }

            // Set ownerUserId for all updated activity types using mapper
            List<ActivityTypeDTO> updatedWithOwner = ActivityTypeMapper.withOwnerUserId(
                activityTypeDTOs.getUpdatedActivityTypes(), 
                userId
            );

            BatchActivityTypeUpdateDTO batchWithOwner = new BatchActivityTypeUpdateDTO(
                updatedWithOwner,
                activityTypeDTOs.getDeletedActivityTypeIds()
            );

            // Reuse existing logic
            return updateActivityTypes(batchWithOwner);
        } catch (Exception e) {
            logger.error("Error batch updating activity types for user " + userId + ": " + e.getMessage());
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


}
