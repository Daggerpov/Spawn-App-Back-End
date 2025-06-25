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
    public ActivityTypeDTO createActivityType(ActivityTypeDTO activityTypeDTO) {
        try {
            logger.info("Creating activity type: " + activityTypeDTO.getTitle() + " for user: " + activityTypeDTO.getOwnerUserId());
            
            // Validate that the user exists
            User creator = userService.getUserEntityById(activityTypeDTO.getOwnerUserId());
            
            // Create the activity type entity
            ActivityType activityType = ActivityTypeMapper.toEntity(activityTypeDTO, creator);
            setOrderNumber(activityType);
            
            // Save the activity type
            ActivityType savedActivityType = repository.save(activityType);
            
            logger.info("Successfully created activity type " + savedActivityType.getId() + " for user " + activityTypeDTO.getOwnerUserId());
            return ActivityTypeMapper.toDTO(savedActivityType);
            
        } catch (Exception e) {
            logger.error("Error creating activity type for user " + activityTypeDTO.getOwnerUserId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to create activity type", e);
        }
    }

    @Override
    public BatchActivityTypeUpdateDTO updateActivityTypes(BatchActivityTypeUpdateDTO activityTypeDTOs) {
        try {
            // Extract userId from the first activity type in the batch (they should all belong to the same user)
            if (activityTypeDTOs.getUpdatedActivityTypes().isEmpty() && activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                throw new IllegalArgumentException("No activity types to update or delete");
            }
            
            UUID userId = null;
            if (!activityTypeDTOs.getUpdatedActivityTypes().isEmpty()) {
                userId = activityTypeDTOs.getUpdatedActivityTypes().get(0).getOwnerUserId();
            } else {
                // If only deleting, we need to get userId from an existing activity type
                ActivityType existingActivityType = repository.findById(activityTypeDTOs.getDeletedActivityTypeIds().get(0))
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ActivityType, activityTypeDTOs.getDeletedActivityTypeIds().get(0)));
                userId = existingActivityType.getCreator().getId();
            }
            
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
    public ActivityTypeDTO togglePin(UUID activityTypeId, UUID userId) {
        try {
            logger.info("Toggling pin status for activity type: " + activityTypeId + " by user: " + userId);
            
            // Find the activity type
            ActivityType activityType = repository.findById(activityTypeId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ActivityType, activityTypeId));
            
            // Verify that the user owns this activity type
            if (!activityType.getCreator().getId().equals(userId)) {
                throw new SecurityException("User " + userId + " is not authorized to modify activity type " + activityTypeId);
            }
            
            // Toggle the pinned status
            boolean newPinnedStatus = !activityType.getIsPinned();
            activityType.setIsPinned(newPinnedStatus);
            
            // Save the updated activity type
            ActivityType savedActivityType = repository.save(activityType);
            
            logger.info("Successfully toggled pin status for activity type " + activityTypeId + " to " + newPinnedStatus);
            return ActivityTypeMapper.toDTO(savedActivityType);
            
        } catch (BaseNotFoundException | SecurityException e) {
            logger.error("Error toggling pin for activity type " + activityTypeId + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error toggling pin for activity type " + activityTypeId + ": " + e.getMessage());
            throw new RuntimeException("Failed to toggle pin status", e);
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
