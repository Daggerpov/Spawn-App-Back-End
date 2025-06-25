package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ActivityTypeMapper;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.UserActivityTypePin;
import com.danielagapov.spawn.Repositories.IActivityTypeRepository;
import com.danielagapov.spawn.Repositories.IUserActivityTypePinRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ActivityTypeService implements IActivityTypeService {
    private IActivityTypeRepository repository;
    private IUserActivityTypePinRepository userActivityTypePinRepository;
    private ILogger logger;
    private IUserService userService;

    @Override
    public List<ActivityTypeDTO> getActivityTypesByUserId(UUID userId) {
        return ActivityTypeMapper.toDTOList(repository.findActivityTypesByCreatorId(userId));
    }

    @Override
    public List<UUID> getPinnedActivityTypeIds(UUID userId) {
        try {
            logger.info("Fetching pinned activity type IDs for user: " + userId);
            return userActivityTypePinRepository.findPinnedActivityTypeIdsByUserId(userId);
        } catch (Exception e) {
            logger.error("Error fetching pinned activity type IDs for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to fetch pinned activity types", e);
        }
    }

    @Override
    @Transactional
    public void toggleActivityTypePin(UUID userId, UUID activityTypeId, Boolean isPinned) {
        try {
            logger.info("Toggling pin status for activity type " + activityTypeId + " to " + isPinned + " for user: " + userId);
            
            // Validate that the activity type exists
            ActivityType activityType = repository.findById(activityTypeId)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ActivityType, activityTypeId));
            
            // Validate that the user exists
            User user = userService.getUserEntityById(userId);
            
            boolean currentlyPinned = userActivityTypePinRepository.existsByUserIdAndActivityTypeId(userId, activityTypeId);
            
            if (isPinned && !currentlyPinned) {
                // Pin the activity type
                UserActivityTypePin pin = new UserActivityTypePin(user, activityType);
                userActivityTypePinRepository.save(pin);
                logger.info("Successfully pinned activity type " + activityTypeId + " for user " + userId);
            } else if (!isPinned && currentlyPinned) {
                // Unpin the activity type
                userActivityTypePinRepository.deleteByUserIdAndActivityTypeId(userId, activityTypeId);
                logger.info("Successfully unpinned activity type " + activityTypeId + " for user " + userId);
            } else {
                logger.info("Activity type " + activityTypeId + " pin status already matches desired state (" + isPinned + ") for user " + userId);
            }
        } catch (BaseNotFoundException e) {
            logger.error("Entity not found when toggling pin status: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error toggling pin status for activity type " + activityTypeId + " for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to toggle activity type pin status", e);
        }
    }

    @Override
    public ActivityTypeDTO createActivityType(UUID userId, ActivityTypeDTO activityTypeDTO) {
        try {
            logger.info("Creating activity type: " + activityTypeDTO.getTitle() + " for user: " + userId);
            
            // Validate that the user exists
            User creator = userService.getUserEntityById(userId);
            
            // Create the activity type entity
            ActivityType activityType = ActivityTypeMapper.toEntity(activityTypeDTO, creator);
            setOrderNumber(activityType);
            
            // Save the activity type
            ActivityType savedActivityType = repository.save(activityType);
            
            logger.info("Successfully created activity type " + savedActivityType.getId() + " for user " + userId);
            return ActivityTypeMapper.toDTO(savedActivityType);
            
        } catch (Exception e) {
            logger.error("Error creating activity type for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to create activity type", e);
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
