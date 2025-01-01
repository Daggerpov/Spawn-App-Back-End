package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Models.FriendTag;

import java.util.List;
import java.util.stream.Collectors;

public class FriendTagMapper {
    public static FriendTagDTO toDTO(FriendTag entity) {
        return new FriendTagDTO(
                entity.getId(),
                entity.getDisplayName(),
                entity.getColorHexCode(),
                entity.getOwner(),
                userService.getUserFriendsId(entity.getOwner())
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        return new FriendTag(
                dto.id(),
                dto.displayName(),
                dto.colorHexCode(),
                UserMapper.toEntity(userService.getUserById(dto.owner())).getId()
        );
    }

    public static List<FriendTagDTO> toDTOList(List<FriendTag> entities,) {
        return entities.stream()
                .map(friendTag -> toDTO(friendTag, userService))
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> dtos, ) {
        return dtos.stream()
                .map(ft -> toEntity(ft, userService))
                .collect(Collectors.toList());
    }
}
