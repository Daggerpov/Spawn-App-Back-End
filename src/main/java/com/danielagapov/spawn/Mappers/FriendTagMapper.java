package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Exceptions.Base.DTOMappingException;
import com.danielagapov.spawn.Models.FriendTag;

import java.util.List;
import java.util.stream.Collectors;

import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.User.UserService;

public class FriendTagMapper {
    public static FriendTagDTO toDTO(FriendTag entity, UserService userService) {
        /*List<UserDTO> friends = uft_repository.findFriendIdsByTagId(entity.getId())
                .stream()
                .map(uuid -> UserMapper.toDTO(user_repository.findById(uuid).orElseThrow(() ->
                    new DTOMappingException("Failed to map friend tag to friends"))))
                .collect(Collectors.toList());
        UserDTO owner = UserMapper.toDTO(user_repository.findById(entity.getOwner()).orElseThrow(() ->
                new DTOMappingException("Failed to map owner to from friend tag")));*/
        return new FriendTagDTO(
                entity.getId(),
                entity.getDisplayName(),
                entity.getColorHexCode(),
                userService.getUserById(entity.getOwner()),
                userService.getUserFriends(entity.getOwner())
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        return new FriendTag(
                dto.id(),
                dto.displayName(),
                dto.colorHexCode(),
                UserMapper.toEntity(dto.owner()).getId()
        );
    }

    public static List<FriendTagDTO> toDTOList(List<FriendTag> entities, UserService userService) {
        return entities.stream()
                .map(friendTag -> toDTO(friendTag, userService))
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> dtos) {
        return dtos.stream()
                .map(FriendTagMapper::toEntity)
                .collect(Collectors.toList());
    }
}
