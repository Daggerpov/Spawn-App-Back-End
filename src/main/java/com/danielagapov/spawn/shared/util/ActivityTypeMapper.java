package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.activity.api.dto.ActivityTypeDTO;
import com.danielagapov.spawn.activity.internal.domain.ActivityType;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ActivityTypeMapper {

    public static ActivityType toEntity(ActivityTypeDTO dto, User creator) {
        return new ActivityType(
                dto.getId(),
                dto.getTitle(),
                dto.getAssociatedFriends() != null ? UserMapper.toEntityList(dto.getAssociatedFriends()) : Collections.emptyList(),
                creator,
                dto.getOrderNum(),
                dto.getIcon(),
                dto.getIsPinned() != null ? dto.getIsPinned() : false
        );
    }

    public static List<ActivityType> toEntityList(List<ActivityTypeDTO> dtos, User creator) {
        return dtos.stream()
                .map(dto -> toEntity(dto, creator))
                .toList();
    }

    public static List<ActivityTypeDTO> toDTOList(List<ActivityType> entities) {
        return entities.stream()
                .map(ActivityTypeMapper::toDTO)
                .toList();
    }

    public static ActivityTypeDTO toDTO(ActivityType entity) {
        return new ActivityTypeDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getAssociatedFriends() != null ? UserMapper.toDTOList(entity.getAssociatedFriends()) : Collections.emptyList(),
                entity.getIcon(),
                entity.getOrderNum(),
                entity.getCreator().getId(),
                entity.getIsPinned()
        );
    }

    /**
     * Map ActivityTypeDTO with ownerUserId for batch operations
     */
    public static ActivityTypeDTO withOwnerUserId(ActivityTypeDTO dto, UUID ownerUserId) {
        return new ActivityTypeDTO(
                dto.getId(),
                dto.getTitle(),
                dto.getAssociatedFriends(),
                dto.getIcon(),
                dto.getOrderNum(),
                ownerUserId,
                dto.getIsPinned() != null ? dto.getIsPinned() : false
        );
    }

    /**
     * Map list of ActivityTypeDTOs with ownerUserId for batch operations
     */
    public static List<ActivityTypeDTO> withOwnerUserId(List<ActivityTypeDTO> dtos, UUID ownerUserId) {
        return dtos.stream()
                .map(dto -> withOwnerUserId(dto, ownerUserId))
                .toList();
    }
}
