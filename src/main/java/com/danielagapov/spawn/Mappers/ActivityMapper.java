package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.Activity.ActivityDTO;
import com.danielagapov.spawn.DTOs.Activity.FullFeedActivityDTO;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.User.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ActivityMapper {

    // Convert entity to DTO
    public static ActivityDTO toDTO(Activity entity, UUID creatorUserId, List<UUID> participantUserIds, List<UUID> invitedUserIds, List<UUID> chatMessageIds, boolean isExpired) {
        return new ActivityDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getLocation() != null ? LocationMapper.toDTO(entity.getLocation()) : null, // location (full DTO)
                entity.getActivityType() != null ? entity.getActivityType().getId() : null, // Map ActivityType to ActivityTypeDTO
                entity.getNote(),
                entity.getIcon(),
                entity.getParticipantLimit(),
                creatorUserId,
                participantUserIds,
                invitedUserIds,
                chatMessageIds,
                entity.getCreatedAt(),
                isExpired
        );
    }

    // Convert DTO to entity
    public static Activity toEntity(ActivityDTO dto, Location location, User creator, ActivityType activityType) {
        Activity activity = new Activity(
                dto.getId(),
                dto.getTitle(),
                dto.getStartTime(),
                dto.getEndTime(),
                location, // Assign the full Location entity
                dto.getNote(),
                creator,
                dto.getIcon()
        );
        activity.setActivityType(activityType); // Set the ActivityType entity
        activity.setParticipantLimit(dto.getParticipantLimit()); // Set the participant limit
        // Set createdAt if it exists in the DTO, otherwise it will be set by @PrePersist
        if (dto.getCreatedAt() != null) {
            activity.setCreatedAt(dto.getCreatedAt());
        }
        return activity;
    }

    public static List<ActivityDTO> toDTOList(
            List<Activity> entities,
            Map<UUID, UUID> creatorUserIdMap, // Map of Activity ID to creatorUserId UserDTO
            Map<UUID, List<UUID>> participantUserIdsMap, // Map of Activity ID to participantUserIds
            Map<UUID, List<UUID>> invitedUserIdsMap, // Map of Activity ID to invitedUserIds users
            Map<UUID, List<UUID>> chatMessageIdsMap, // Map of Activity ID to chat messages
            Map<UUID, Boolean> isExpiredMap // Map of Activity ID to expiration status
    ) {
        return entities.stream()
                .map(entity -> toDTO(
                        entity,
                        creatorUserIdMap.get(entity.getId()), // Fetch the creatorUserId UserDTO
                        participantUserIdsMap.getOrDefault(entity.getId(), List.of()), // Fetch participantUserIds or default empty
                        invitedUserIdsMap.getOrDefault(entity.getId(), List.of()), // Fetch invitedUserIds users or default empty
                        chatMessageIdsMap.getOrDefault(entity.getId(), List.of()), // Fetch chat messages or default empty
                        isExpiredMap.getOrDefault(entity.getId(), false) // Fetch expiration status or default to false
                ))
                .collect(Collectors.toList());
    }

    public static List<Activity> toEntityList(List<ActivityDTO> activityDTOS, List<Location> locations, List<User> creators, List<ActivityType> activityTypes) {
        return activityDTOS.stream()
                .map(dto -> {
                    // Find the Location entity based on the location DTO from ActivityDTO
                    Location location = null;
                    if (dto.getLocation() != null) {
                        location = locations.stream()
                                .filter(loc -> loc.getId().equals(dto.getLocation().getId())) // Match LocationDTO's UUID with Location entity
                                .findFirst()
                                .orElse(null);
                    }

                    // Find the User entity (creator) based on the creatorUserId from DTO
                    User creator = creators.stream()
                            .filter(user -> user.getId().equals(dto.getCreatorUserId())) // Match creatorUserId with User entity
                            .findFirst()
                            .orElse(null);

                    // Find the ActivityType entity based on the activityTypeId from DTO
                    ActivityType activityType = dto.getActivityTypeId() != null 
                            ? activityTypes.stream()
                                    .filter(type -> type.getId().equals(dto.getActivityTypeId()))
                                    .findFirst()
                                    .orElse(null)
                            : null;

                    return toEntity(dto, location, creator, activityType); // Convert DTO to entity
                })
                .collect(Collectors.toList());
    }

    public static Activity convertFullFeedActivityDTOToActivityEntity(FullFeedActivityDTO dto) {
        Activity activity = new Activity();
        activity.setId(dto.getId()); // Set the UUID
        activity.setTitle(dto.getTitle()); // Set the title
        activity.setStartTime(dto.getStartTime()); // Set the start time
        activity.setEndTime(dto.getEndTime()); // Set the end time
        activity.setIcon(dto.getIcon()); // Set the icon

        // Convert LocationDTO to Location entity (assuming a similar method exists)
        Location location = LocationMapper.toEntity(dto.getLocation());
        activity.setLocation(location); // Set the location

        activity.setNote(dto.getNote()); // Set the note
        activity.setParticipantLimit(dto.getParticipantLimit()); // Set the participant limit

        // Convert BaseUserDTO to User entity (assuming a similar method exists)
        User creator = UserMapper.toEntity(dto.getCreatorUser());
        activity.setCreator(creator); // Set the creator

        // Set createdAt if it exists in the DTO, otherwise it will be set by @PrePersist
        if (dto.getCreatedAt() != null) {
            activity.setCreatedAt(dto.getCreatedAt());
        }

        return activity;
    }


}
