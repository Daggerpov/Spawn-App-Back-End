package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Models.FriendTag;

import java.util.List;
import java.util.stream.Collectors;

import com.danielagapov.spawn.Services.User.UserService;

public class FriendTagMapper {
    public static FriendTagDTO toDTO(FriendTag entity, UserService userService) {
        return new FriendTagDTO(
                entity.getId(),
                entity.getDisplayName(),
                entity.getColorHexCode(),
                entity.getOwner(),
                userService.getUserFriendsId(entity.getOwner())
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto, UserService userService) {
        return new FriendTag(
                dto.id(),
                dto.displayName(),
                dto.colorHexCode(),
                UserMapper.toEntity(userService.getUserById(dto.owner())).getId()
        );
    }

    public static List<FriendTagDTO> toDTOList(List<FriendTag> entities, UserService userService) {
        return entities.stream()
                .map(friendTag -> toDTO(friendTag, userService))
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> dtos, UserService userService) {
        return dtos.stream()
                .map(ft -> toEntity(ft, userService))
                .collect(Collectors.toList());
    }
}
