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
    @Transactional(readOnly = true)
    public List<ActivityTypeDTO> getActivityTypesByUserId(UUID userId) {
        // Returns activity types owned by this user
        return ActivityTypeMapper.toDTOList(repository.findActivityTypesByCreatorId(userId));
    }

    @Override
    @Transactional
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
                
                // Auto-assign order numbers for new activity types
                List<ActivityTypeDTO> processedDTOs = assignOrderNumbersForNewActivityTypes(userId, activityTypeDTOs.getUpdatedActivityTypes());
                
                List<ActivityType> activityTypes = ActivityTypeMapper.toEntityList(processedDTOs, creator);
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
            // Double-check if user already has activity types to avoid duplicate initialization
            List<ActivityType> existingTypes = repository.findActivityTypesByCreatorId(user.getId());
            if (!existingTypes.isEmpty()) {
                logger.info("User " + user.getUsername() + " already has " + existingTypes.size() + " activity types. Skipping initialization.");
                return;
            }
            
            // Get the current max order number for this user
            Integer maxOrder = repository.findMaxOrderNumberByCreatorId(user.getId());
            int startingOrder = maxOrder != null ? maxOrder + 1 : 0;
            
            // Create default activity types with sequential order numbers
            List<ActivityType> defaultActivityTypes = new ArrayList<>();
            
            // Define the default activity types
            String[][] defaultTypes = {
                {"Chill", "🛋️"},
                {"Food", "🍽️"},
                {"Active", "🏃"},
                {"Study", "✏️"}
            };
            
            // Create each activity type with unique order numbers
            for (int i = 0; i < defaultTypes.length; i++) {
                ActivityType activityType = new ActivityType(user, defaultTypes[i][0], defaultTypes[i][1]);
                activityType.setOrderNum(startingOrder + i);
                defaultActivityTypes.add(activityType);
            }
            
            // Save all activity types at once for better performance
            List<ActivityType> savedTypes = repository.saveAll(defaultActivityTypes);
            
            logger.info("Successfully initialized " + savedTypes.size() + " default activity types for user: " + user.getUsername());
            
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
     * Assigns order numbers to new activity types to avoid conflicts
     */
    private List<ActivityTypeDTO> assignOrderNumbersForNewActivityTypes(UUID userId, List<ActivityTypeDTO> activityTypeDTOs) {
        List<ActivityType> existingTypes = repository.findActivityTypesByCreatorId(userId);
        Set<UUID> existingIds = existingTypes.stream()
                .map(ActivityType::getId)
                .collect(Collectors.toSet());
        
        // Get the current max order number
        Integer maxOrder = repository.findMaxOrderNumberByCreatorId(userId);
        int nextOrderNum = maxOrder != null ? maxOrder + 1 : 0;
        
        List<ActivityTypeDTO> processedDTOs = new ArrayList<>();
        
        for (ActivityTypeDTO dto : activityTypeDTOs) {
            if (!existingIds.contains(dto.getId())) {
                // This is a new activity type - assign the next available order number
                ActivityTypeDTO newDTO = new ActivityTypeDTO(
                    dto.getId(),
                    dto.getTitle(),
                    dto.getAssociatedFriends(),
                    dto.getIcon(),
                    nextOrderNum++,  // Auto-assign and increment
                    dto.getOwnerUserId(),
                    dto.getIsPinned()
                );
                processedDTOs.add(newDTO);
                logger.info("Assigned orderNum " + (nextOrderNum - 1) + " to new activity type: " + dto.getTitle());
            } else {
                // This is an existing activity type - keep the original order number
                processedDTOs.add(dto);
            }
        }
        
        return processedDTOs;
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
        
        // Separate existing activity types from new ones for validation
        List<ActivityTypeDTO> existingActivityTypesToUpdate = batchDTO.getUpdatedActivityTypes().stream()
                .filter(dto -> existingIds.contains(dto.getId()))
                .toList();
        
        // Validate orderNum range ONLY for existing activity types being updated
        // New activity types will get auto-assigned order numbers, so skip validation for them
        for (ActivityTypeDTO dto : existingActivityTypesToUpdate) {
            if (dto.getOrderNum() < 0 || dto.getOrderNum() >= finalTotalCount) {
                throw new ActivityTypeValidationException(
                    String.format("Invalid orderNum %d for activity type '%s'. Must be in range [0, %d]", 
                                  dto.getOrderNum(), dto.getTitle(), finalTotalCount - 1)
                );
            }
        }
        
        // Validate orderNum uniqueness within the existing activity types being updated
        Set<Integer> existingUpdateOrderNums = existingActivityTypesToUpdate.stream()
                .map(ActivityTypeDTO::getOrderNum)
                .collect(Collectors.toSet());
        
        if (existingUpdateOrderNums.size() != existingActivityTypesToUpdate.size()) {
            throw new ActivityTypeValidationException(
                "Duplicate orderNum values detected in update. Each activity type must have a unique orderNum."
            );
        }
        
        // Validate orderNum uniqueness against existing activity types that will remain
        // Get existing activity types that will remain after deletions/updates
        Set<UUID> updatedIds = batchDTO.getUpdatedActivityTypes().stream()
                .map(ActivityTypeDTO::getId)
                .collect(Collectors.toSet());
        
        List<ActivityType> remainingExistingTypes = existingActivityTypes.stream()
                .filter(at -> !deletedIds.contains(at.getId()) && !updatedIds.contains(at.getId()))
                .toList();
        
        Set<Integer> existingOrderNums = remainingExistingTypes.stream()
                .map(ActivityType::getOrderNum)
                .collect(Collectors.toSet());
        
        // Check for conflicts between updated orderNums and existing ones
        // Only validate existing activity types being updated, not new ones
        for (Integer orderNum : existingUpdateOrderNums) {
            if (existingOrderNums.contains(orderNum)) {
                throw new ActivityTypeValidationException(
                    String.format("OrderNum %d conflicts with existing activity type. Each activity type must have a unique orderNum.", orderNum)
                );
            }
        }
        
        logger.info(String.format("✅ Activity type validation passed: %d pinned (max %d), orderNum range [0, %d], unique orderNums", 
                                  finalPinnedCount, MAX_PINNED_ACTIVITY_TYPES, finalTotalCount - 1));
    }
}
