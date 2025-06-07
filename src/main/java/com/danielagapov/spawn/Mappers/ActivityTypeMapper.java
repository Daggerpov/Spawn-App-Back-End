package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;

import java.util.List;

public class ActivityTypeMapper {

    public static ActivityType toEntity(ActivityTypeDTO dto, User creator) {
        return new ActivityType(
                dto.getId(),
                dto.getTitle(),
                UserMapper.toEntityList(dto.getAssociatedFriends()),
                creator,
                dto.getOrderNum(),
                dto.getIcon(),
                dto.getColorHexCode()
        );
    }

    public static List<ActivityType> toEntityList(List<ActivityTypeDTO> dtos, User creator) {
        return dtos.stream()
                .map(dto -> toEntity(dto, creator))
                .toList();
    }
}
