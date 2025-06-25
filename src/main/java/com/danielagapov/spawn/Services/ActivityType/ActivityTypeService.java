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
        // Returns activity types owned by this user
        return ActivityTypeMapper.toDTOList(repository.findActivityTypesByCreatorId(userId));
    }



    @Override
    public BatchActivityTypeUpdateDTO updateActivityTypes(BatchActivityTypeUpdateDTO activityTypeDTOs) {
        try {
            if (activityTypeDTOs.getUpdatedActivityTypes().isEmpty() && activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                throw new IllegalArgumentException("No activity types to update or delete");
            }
            
            // Handle deletions first - no need to get userId for deletion
            if (!activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                logger.info("Deleting activity types with IDs: " + activityTypeDTOs.getDeletedActivityTypeIds());
                repository.deleteAllById(activityTypeDTOs.getDeletedActivityTypeIds());
            }
            
            // Handle updates/creations - need userId for these operations
            if (!activityTypeDTOs.getUpdatedActivityTypes().isEmpty()) {
                UUID userId = activityTypeDTOs.getUpdatedActivityTypes().get(0).getOwnerUserId();
                User creator = userService.getUserEntityById(userId);
                
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
    public BatchActivityTypeUpdateDTO updateActivityTypes(UUID userId, BatchActivityTypeUpdateDTO activityTypeDTOs) {
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
