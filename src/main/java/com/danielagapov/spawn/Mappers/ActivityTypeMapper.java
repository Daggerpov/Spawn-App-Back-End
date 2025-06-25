package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;

import java.util.Collections;
import java.util.List;

public class ActivityTypeMapper {

    public static ActivityType toEntity(ActivityTypeDTO dto, User creator) {
        return new ActivityType(
                dto.getId(),
                dto.getTitle(),
                dto.getAssociatedFriends() != null ? UserMapper.toEntityList(dto.getAssociatedFriends()) : Collections.emptyList(),
                creator,
                dto.getOrderNum(),
                dto.getIcon()
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
                entity.getOrderNum()
        );
    }
}
