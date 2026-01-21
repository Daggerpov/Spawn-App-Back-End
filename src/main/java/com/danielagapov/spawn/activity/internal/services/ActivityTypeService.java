package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.activity.api.dto.ActivityTypeDTO;
import com.danielagapov.spawn.activity.api.dto.ActivityTypeFriendSuggestionDTO;
import com.danielagapov.spawn.activity.api.dto.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.exceptions.ActivityTypeValidationException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.ActivityTypeMapper;
import com.danielagapov.spawn.activity.internal.domain.ActivityType;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.activity.internal.repositories.IActivityTypeRepository;
import com.danielagapov.spawn.user.internal.services.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.AbstractUserDTO;
import com.danielagapov.spawn.user.api.dto.FriendUser.MinimalFriendDTO;

@Service
@AllArgsConstructor
public class ActivityTypeService implements IActivityTypeService {
    private IActivityTypeRepository repository;
    private ILogger logger;
    private IUserService userService;

    // Constants for validation
    private static final int MAX_PINNED_ACTIVITY_TYPES = 4;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "activityTypesByUserId", key = "#userId")
    public List<ActivityTypeDTO> getActivityTypesByUserId(UUID userId) {
        // Returns activity types owned by this user
        return ActivityTypeMapper.toDTOList(repository.findActivityTypesByCreatorId(userId));
    }

    @Override
    @Transactional
    @CacheEvict(value = "activityTypesByUserId", key = "#userId")
    public List<ActivityTypeDTO> updateActivityTypes(UUID userId, BatchActivityTypeUpdateDTO activityTypeDTOs) {
        // Synchronize on userId to prevent race conditions for the same user
        synchronized (("activity_type_update_" + userId.toString()).intern()) {
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
                    
                    // Convert DTOs to entities with proper user lookup for associated friends
                    List<ActivityType> activityTypes = convertDTOsToEntitiesWithFriendLookup(processedDTOs, creator);
                    
                    // Use multi-phase update to avoid constraint violations
                    updateActivityTypesWithConstraintHandling(activityTypes, userId);
                }
                
                // Return all activity types for the user after updates
                return getActivityTypesByUserId(userId);
            } catch (Exception e) {
                logger.error("Error batch updating activity types for user " + userId + ": " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Updates activity types using a two-phase approach to avoid unique constraint violations
     * on the (creator_id, order_num) combination during batch updates.
     */
    private void updateActivityTypesWithConstraintHandling(List<ActivityType> activityTypes, UUID userId) {
        // Separate existing and new activity types
        List<ActivityType> existingActivityTypes = activityTypes.stream()
                .filter(at -> repository.existsById(at.getId()))
                .toList();
        
        List<ActivityType> newActivityTypes = activityTypes.stream()
                .filter(at -> !repository.existsById(at.getId()))
                .toList();
        
        // Save new activity types first (they won't have conflicts)
        if (!newActivityTypes.isEmpty()) {
            repository.saveAll(newActivityTypes);
            logger.info("Saved " + newActivityTypes.size() + " new activity types");
        }
        
        // For existing activity types, use multi-phase update to avoid constraint violations
        if (!existingActivityTypes.isEmpty()) {
            updateExistingActivityTypesWithConstraintHandling(existingActivityTypes, userId);
        }
    }
    
    /**
     * Updates existing activity types using a multi-phase approach to avoid unique constraint violations:
     * Phase 1: Store target orderNum values and move all activity types being updated to temporary negative values
     * Phase 2: Move any remaining activity types that conflict with target orderNum values to temporary values
     * Phase 3: Update all activity types to their final orderNum values
     * Phase 4: Reassign new orderNum values to previously conflicting activity types
     */
    @Transactional
    private void updateExistingActivityTypesWithConstraintHandling(List<ActivityType> existingActivityTypes, UUID userId) {
        logger.info("Updating " + existingActivityTypes.size() + " existing activity types with constraint handling");
        
        // Create a map of activity types being updated by their ID for quick lookup
        Map<UUID, ActivityType> updatingActivityTypesMap = existingActivityTypes.stream()
                .collect(Collectors.toMap(ActivityType::getId, at -> at));
        
        // Store the target orderNum values before modifying the objects
        Map<UUID, Integer> targetOrderNums = existingActivityTypes.stream()
                .collect(Collectors.toMap(ActivityType::getId, ActivityType::getOrderNum));
        
        // Get the set of target orderNum values for conflict detection
        Set<Integer> targetOrderNumValues = new HashSet<>(targetOrderNums.values());
        
        // Phase 1: Move all activity types being updated to temporary negative orderNum values
        for (int i = 0; i < existingActivityTypes.size(); i++) {
            ActivityType activityType = existingActivityTypes.get(i);
            int tempOrderNum = -(i + 1000); // Use negative numbers starting from -1000
            
            logger.info("Phase 1: Moving '" + activityType.getTitle() + "' to temporary orderNum " + tempOrderNum);
            activityType.setOrderNum(tempOrderNum);
            repository.save(activityType);
        }
        
        // Phase 2: Refresh the list to get current state after Phase 1, then move any remaining conflicting activity types
        List<ActivityType> currentUserActivityTypes = repository.findActivityTypesByCreatorId(userId);
        List<ActivityType> conflictingActivityTypes = currentUserActivityTypes.stream()
                .filter(at -> !updatingActivityTypesMap.containsKey(at.getId())) // Not being updated in this batch
                .filter(at -> targetOrderNumValues.contains(at.getOrderNum())) // Has a conflicting orderNum
                .toList();
        
        for (int i = 0; i < conflictingActivityTypes.size(); i++) {
            ActivityType conflictingType = conflictingActivityTypes.get(i);
            int originalOrderNum = conflictingType.getOrderNum();
            int tempOrderNum = -(i + 2000); // Use different negative range to avoid conflicts
            
            logger.info("Phase 2: Moving conflicting activity type '" + conflictingType.getTitle() + 
                       "' from orderNum " + originalOrderNum + " to temporary orderNum " + tempOrderNum);
            conflictingType.setOrderNum(tempOrderNum);
            repository.save(conflictingType);
        }
        
        // Phase 3: Update activity types being updated to their final orderNum values
        for (ActivityType activityType : existingActivityTypes) {
            Integer finalOrderNum = targetOrderNums.get(activityType.getId());
            activityType.setOrderNum(finalOrderNum);
            
            logger.info("Phase 3: Setting final orderNum " + finalOrderNum + " for activity type: " + activityType.getTitle());
            repository.save(activityType);
        }
        
        // Phase 4: Find new orderNum values for the conflicting activity types and update them
        if (!conflictingActivityTypes.isEmpty()) {
            // Get the max orderNum after all updates
            Integer maxOrderNum = repository.findMaxOrderNumberByCreatorId(userId);
            int nextAvailableOrderNum = maxOrderNum != null ? maxOrderNum + 1 : 0;
            
            for (ActivityType conflictingType : conflictingActivityTypes) {
                logger.info("Phase 4: Reassigning conflicting activity type '" + conflictingType.getTitle() + 
                           "' to new orderNum " + nextAvailableOrderNum);
                conflictingType.setOrderNum(nextAvailableOrderNum++);
                repository.save(conflictingType);
            }
        }
        
        logger.info("Successfully completed multi-phase update for existing activity types");
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
            int startingOrder = maxOrder != null ? maxOrder + 1 : 1; // 1-based indexing
            
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
        activityType.setOrderNum(maxOrder != null ? maxOrder + 1 : 1); // 1-based indexing
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
        int nextOrderNum = maxOrder != null ? maxOrder + 1 : 1; // 1-based indexing
        
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
        // Get current state - ensure we have the most up-to-date data
        repository.flush(); // Ensure any pending changes are persisted before validation
        long currentPinnedCount = repository.countByCreatorIdAndIsPinnedTrue(userId);
        long currentTotalCount = repository.countByCreatorId(userId);
        
        // Calculate final state after update
        long deletedCount = batchDTO.getDeletedActivityTypeIds().size();
        long updatedCount = batchDTO.getUpdatedActivityTypes().size();
        
        // Get existing activity types and categorize them
        List<ActivityType> existingActivityTypes = repository.findActivityTypesByCreatorId(userId);
        Set<UUID> deletedIds = Set.copyOf(batchDTO.getDeletedActivityTypeIds());
        Set<UUID> updatedIds = batchDTO.getUpdatedActivityTypes().stream()
                .map(ActivityTypeDTO::getId)
                .collect(Collectors.toSet());
        
        // Count currently pinned activity types that will be deleted
        long deletedPinnedCount = existingActivityTypes.stream()
                .filter(at -> deletedIds.contains(at.getId()) && at.getIsPinned())
                .count();
        
        // Count currently pinned activity types that will remain unchanged (not updated or deleted)
        long unchangedPinnedCount = existingActivityTypes.stream()
                .filter(at -> !deletedIds.contains(at.getId()) && !updatedIds.contains(at.getId()) && at.getIsPinned())
                .count();
        
        // Count activity types in the update request that are marked as pinned
        long updatedPinnedCount = batchDTO.getUpdatedActivityTypes().stream()
                .filter(dto -> dto.getIsPinned() != null && dto.getIsPinned())
                .count();
        
        // Debug logging for request details
        logger.info("=== VALIDATION DEBUG ===");
        logger.info("Current state: " + currentPinnedCount + " pinned, " + currentTotalCount + " total");
        logger.info("Request deletions: " + deletedCount + " (pinned deletions: " + deletedPinnedCount + ")");
        logger.info("Request updates: " + updatedCount);
        logger.info("Unchanged pinned activity types: " + unchangedPinnedCount);
        
        // Log each updated activity type's pinned status
        for (ActivityTypeDTO dto : batchDTO.getUpdatedActivityTypes()) {
            logger.info("Update request - ID: " + dto.getId() + ", Title: " + dto.getTitle() + 
                       ", isPinned: " + dto.getIsPinned() + ", orderNum: " + dto.getOrderNum());
        }
        
        logger.info("Updated items marked as pinned: " + updatedPinnedCount);
        
        // Calculate final pinned count correctly (unchanged + updated, no double counting)
        long finalPinnedCount = unchangedPinnedCount + updatedPinnedCount;
        
        logger.info("Calculation: " + unchangedPinnedCount + " (unchanged) + " + updatedPinnedCount + " (updated) = " + finalPinnedCount);
        logger.info("=== END VALIDATION DEBUG ===");
        
        // Calculate final total count (accounting for new vs existing updates)
        Set<UUID> existingIds = existingActivityTypes.stream()
                .map(ActivityType::getId)
                .collect(Collectors.toSet());
        long newActivityTypeCount = batchDTO.getUpdatedActivityTypes().stream()
                .filter(dto -> !existingIds.contains(dto.getId()))
                .count();
        long finalTotalCount = currentTotalCount - deletedCount + newActivityTypeCount;
        boolean hasNewCreations = newActivityTypeCount > 0;
        
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
        
        // Validate orderNum range ONLY for existing activity types being updated (1-based inclusive range)
        for (ActivityTypeDTO dto : existingActivityTypesToUpdate) {
            if (dto.getOrderNum() < 1 || dto.getOrderNum() > finalTotalCount) {
                throw new ActivityTypeValidationException(
                    String.format("Invalid orderNum %d for activity type '%s'. Must be in range [1, %d]", 
                                  dto.getOrderNum(), dto.getTitle(), finalTotalCount)
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
        
        List<ActivityType> remainingExistingActivityTypes = existingActivityTypes.stream()
                .filter(at -> !deletedIds.contains(at.getId()) && !updatedIds.contains(at.getId()))
                .toList();
        
        Set<Integer> remainingOrderNums = remainingExistingActivityTypes.stream()
                .map(ActivityType::getOrderNum)
                .collect(Collectors.toSet());
        
        // Check for conflicts between updated activity types and remaining ones
        for (Integer orderNum : existingUpdateOrderNums) {
            if (remainingOrderNums.contains(orderNum)) {
                throw new ActivityTypeValidationException(
                    String.format("orderNum %d conflicts with existing activity type. Each activity type must have a unique orderNum.", 
                                  orderNum)
                );
            }
        }
        
        logger.info("Activity type validation passed: " + finalPinnedCount + " pinned (max " + MAX_PINNED_ACTIVITY_TYPES + "), " +
                    "orderNum range [1, " + (finalTotalCount) + "], unique orderNums");
    }

    /**
     * Convert DTOs to entities with proper user lookup for associated friends
     * This prevents issues with detached entities by fetching users from the database
     */
    private List<ActivityType> convertDTOsToEntitiesWithFriendLookup(List<ActivityTypeDTO> dtos, User creator) {
        return dtos.stream()
                .map(dto -> convertDTOToEntityWithFriendLookup(dto, creator))
                .toList();
    }

    /**
     * Convert a single DTO to entity with proper user lookup for associated friends
     */
    private ActivityType convertDTOToEntityWithFriendLookup(ActivityTypeDTO dto, User creator) {
        // Get associated friends from database instead of creating detached entities
        List<User> associatedFriends = new ArrayList<>();
        if (dto.getAssociatedFriends() != null && !dto.getAssociatedFriends().isEmpty()) {
            for (MinimalFriendDTO friendDTO : dto.getAssociatedFriends()) {
                try {
                    User friend = userService.getUserEntityById(friendDTO.getId());
                    associatedFriends.add(friend);
                } catch (Exception e) {
                    // Log the error and skip this friend if they don't exist
                    logger.error("Skipping associated friend with ID " + friendDTO.getId() + 
                               " for activity type '" + dto.getTitle() + "' as user not found: " + e.getMessage());
                }
            }
        }

        return new ActivityType(
                dto.getId(),
                dto.getTitle(),
                associatedFriends,
                creator,
                dto.getOrderNum(),
                dto.getIcon(),
                dto.getIsPinned() != null ? dto.getIsPinned() : false
        );
    }
    
    @Override
    public ActivityTypeFriendSuggestionDTO getFriendSuggestionsForActivityType(UUID activityTypeId, UUID userId) {
        try {
            // Get the activity type
            ActivityType activityType = repository.findById(activityTypeId).orElse(null);
            if (activityType == null || !activityType.getCreator().getId().equals(userId)) {
                return null;
            }
            
            // Check if activity type has no associated friends
            if (activityType.getAssociatedFriends() != null && !activityType.getAssociatedFriends().isEmpty()) {
                return null; // Activity type already has friends, no suggestion needed
            }
            
            // Get user's friends
            List<? extends AbstractUserDTO> userFriends = userService.getFullFriendUsersByUserId(userId);
            if (userFriends == null || userFriends.isEmpty()) {
                return null; // User has no friends to suggest
            }
            
            // Convert to BaseUserDTO list
            List<BaseUserDTO> suggestedFriends = userFriends.stream()
                    .map(friend -> (BaseUserDTO) friend)
                    .collect(Collectors.toList());
            
            return new ActivityTypeFriendSuggestionDTO(
                    activityTypeId,
                    activityType.getTitle(),
                    suggestedFriends
            );
            
        } catch (Exception e) {
            logger.error("Error getting friend suggestions for activity type " + activityTypeId + ": " + e.getMessage());
            return null;
        }
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "activityTypesByUserId", key = "#userId")
    public boolean autoAddFriendsToActivityType(UUID activityTypeId, UUID userId) {
        try {
            // Get the activity type
            ActivityType activityType = repository.findById(activityTypeId).orElse(null);
            if (activityType == null || !activityType.getCreator().getId().equals(userId)) {
                return false;
            }
            
            // Check if activity type already has associated friends
            if (activityType.getAssociatedFriends() != null && !activityType.getAssociatedFriends().isEmpty()) {
                return false; // Activity type already has friends, no need to add
            }
            
            // Get user's friends
            List<? extends AbstractUserDTO> userFriends = userService.getFullFriendUsersByUserId(userId);
            if (userFriends == null || userFriends.isEmpty()) {
                return false; // User has no friends to add
            }
            
            // Convert friend DTOs to User entities
            List<User> friendEntities = new ArrayList<>();
            for (AbstractUserDTO friendDTO : userFriends) {
                try {
                    User friend = userService.getUserEntityById(friendDTO.getId());
                    friendEntities.add(friend);
                } catch (Exception e) {
                    logger.warn("Skipping friend with ID " + friendDTO.getId() + " as user not found: " + e.getMessage());
                }
            }
            
            if (friendEntities.isEmpty()) {
                return false;
            }
            
            // Add friends to activity type
            activityType.setAssociatedFriends(friendEntities);
            repository.save(activityType);
            
            logger.info("Automatically added " + friendEntities.size() + " friends to activity type '" + 
                       activityType.getTitle() + "' for user " + userId);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error auto-adding friends to activity type " + activityTypeId + ": " + e.getMessage());
            return false;
        }
    }
}
