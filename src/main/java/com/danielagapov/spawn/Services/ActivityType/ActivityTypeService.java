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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public List<ActivityTypeDTO> updateActivityTypes(UUID userId, BatchActivityTypeUpdateDTO activityTypeDTOs) {
        try {
            if (activityTypeDTOs.getUpdatedActivityTypes().isEmpty() && activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                throw new IllegalArgumentException("No activity types to update or delete");
            }

            // Validate if there are updates
            if (!activityTypeDTOs.getUpdatedActivityTypes().isEmpty()) {
                validateActivityTypeUpdate(userId, activityTypeDTOs);
            }
            
            // Handle deletions first
            if (!activityTypeDTOs.getDeletedActivityTypeIds().isEmpty()) {
                logger.info("Deleting activity types with IDs: " + activityTypeDTOs.getDeletedActivityTypeIds());
                repository.deleteAllById(activityTypeDTOs.getDeletedActivityTypeIds());
            }
            
            // Handle updates/creations
            if (!activityTypeDTOs.getUpdatedActivityTypes().isEmpty()) {
                User creator = userService.getUserEntityById(userId);
                
                logger.info("Saving updated or newly created activity types for user: " + creator.getUsername());
                List<ActivityType> activityTypes = ActivityTypeMapper.toEntityList(activityTypeDTOs.getUpdatedActivityTypes(), creator);
                repository.saveAll(activityTypes);
            }
            
            // Return all activity types for the user after updates
            return getActivityTypesByUserId(userId);
        } catch (Exception e) {
            logger.error("Error batch updating activity types for user " + userId + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void initializeDefaultActivityTypesForUser(User user) {
        try {
            // Get the current max order number for this user
            Integer maxOrder = repository.findMaxOrderNumberByCreatorId(user.getId());
            int startingOrder = maxOrder != null ? maxOrder + 1 : 0;
            
            // Create default activity types with sequential order numbers
            List<ActivityType> defaultActivityTypes = new ArrayList<>();
            
            ActivityType chillType = new ActivityType(user, "Chill","🛋️");
            chillType.setOrderNum(startingOrder);
            defaultActivityTypes.add(chillType);
            
            ActivityType foodType = new ActivityType(user, "Food", "🍽️");
            foodType.setOrderNum(startingOrder + 1);
            defaultActivityTypes.add(foodType);
            
            ActivityType activeType = new ActivityType(user, "Active", "🏃");
            activeType.setOrderNum(startingOrder + 2);
            defaultActivityTypes.add(activeType);
            
            ActivityType studyType = new ActivityType(user, "Study", "✏️");
            studyType.setOrderNum(startingOrder + 3);
            defaultActivityTypes.add(studyType);
            
            // Save all activity types in a single transaction
            repository.saveAll(defaultActivityTypes);
            
            logger.info("Successfully initialized " + defaultActivityTypes.size() + " default activity types for user: " + user.getUsername());
            
        } catch (Exception e) {
            logger.error("Failed to initialize default activity types for user " + user.getUsername() + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void setOrderNumber(ActivityType activityType) {
        Integer maxOrder = repository.findMaxOrderNumberByCreatorId(activityType.getCreator().getId());
        activityType.setOrderNum(maxOrder != null ? maxOrder + 1 : 0);
    }

    /**
     * Validates activity type updates for pinned count, orderNum range, and orderNum uniqueness
     */
    private void validateActivityTypeUpdate(UUID userId, BatchActivityTypeUpdateDTO batchDTO) {
        // Get current state
        long currentPinnedCount = repository.countByCreatorIdAndIsPinnedTrue(userId);
        long currentTotalCount = repository.countByCreatorId(userId);
        
        // Calculate final state after update
        long deletedCount = batchDTO.getDeletedActivityTypeIds().size();
        long updatedCount = batchDTO.getUpdatedActivityTypes().size();
        
        // Count how many of the deleted items are pinned
        List<ActivityType> existingActivityTypes = repository.findActivityTypesByCreatorId(userId);
        Set<UUID> deletedIds = Set.copyOf(batchDTO.getDeletedActivityTypeIds());
        long deletedPinnedCount = existingActivityTypes.stream()
                .filter(at -> deletedIds.contains(at.getId()) && at.getIsPinned())
                .count();
        
        // Count how many updated items are pinned
        long updatedPinnedCount = batchDTO.getUpdatedActivityTypes().stream()
                .filter(dto -> dto.getIsPinned() != null && dto.getIsPinned())
                .count();
        
        // Calculate final pinned count
        long finalPinnedCount = currentPinnedCount - deletedPinnedCount + updatedPinnedCount;
        
        // Calculate final total count (accounting for new vs existing updates)
        Set<UUID> existingIds = existingActivityTypes.stream()
                .map(ActivityType::getId)
                .collect(Collectors.toSet());
        long newActivityTypeCount = batchDTO.getUpdatedActivityTypes().stream()
                .filter(dto -> !existingIds.contains(dto.getId()))
                .count();
        long finalTotalCount = currentTotalCount - deletedCount + newActivityTypeCount;
        
        // Validate pinned count
        if (finalPinnedCount > MAX_PINNED_ACTIVITY_TYPES) {
            throw new ActivityTypeValidationException(
                String.format("Cannot have more than %d pinned activity types. Requested: %d", 
                              MAX_PINNED_ACTIVITY_TYPES, finalPinnedCount)
            );
        }
        
        // Validate orderNum range for all updated activity types
        for (ActivityTypeDTO dto : batchDTO.getUpdatedActivityTypes()) {
            if (dto.getOrderNum() < 0 || dto.getOrderNum() >= finalTotalCount) {
                throw new ActivityTypeValidationException(
                    String.format("Invalid orderNum %d for activity type '%s'. Must be in range [0, %d]", 
                                  dto.getOrderNum(), dto.getTitle(), finalTotalCount - 1)
                );
            }
        }
        
        // Validate orderNum uniqueness within the updated activity types
        Set<Integer> orderNums = batchDTO.getUpdatedActivityTypes().stream()
                .map(ActivityTypeDTO::getOrderNum)
                .collect(Collectors.toSet());
        
        if (orderNums.size() != batchDTO.getUpdatedActivityTypes().size()) {
            throw new ActivityTypeValidationException(
                "Duplicate orderNum values detected in update. Each activity type must have a unique orderNum."
            );
        }
        
        logger.info(String.format("✅ Activity type validation passed: %d pinned (max %d), orderNum range [0, %d], unique orderNums", 
                                  finalPinnedCount, MAX_PINNED_ACTIVITY_TYPES, finalTotalCount - 1));
    }
}
