package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.FriendTag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FriendTagMapper {
    public static FriendTagDTO toDTO(FriendTag entity, UserDTO owner, List<UserDTO> friends) {
        return new FriendTagDTO(
                entity.getId(),
                entity.getDisplayName(),
                entity.getColorHexCode(),
                owner,
                friends
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        return new FriendTag(
                dto.id(),
                dto.displayName(),
                dto.colorHexCode(),
                dto.owner().id()
        );
    }

    public static List<FriendTagDTO> toDTOList(
            List<FriendTag> entities,
            Map<FriendTag, UserDTO> ownerMap,
            Map<FriendTag, List<UserDTO>> friendsMap
    ) {
        return entities.stream()
                .map(friendTag -> toDTO(
                        friendTag,
                        ownerMap.getOrDefault(friendTag, null), // Default to null if owner is missing
                        friendsMap.getOrDefault(friendTag, List.of()) // Default to an empty list if friends are missing
                ))
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> friendTagDTOs) {
        return friendTagDTOs.stream()
                .map(FriendTagMapper::toEntity)
                .collect(Collectors.toList());
    }
}
