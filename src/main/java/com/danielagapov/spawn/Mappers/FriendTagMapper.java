package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagCreationDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.Models.FriendTag;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class FriendTagMapper {
    public static FriendTagDTO toDTO(FriendTag entity, UUID ownerUserId, List<UUID> friendUserIds) {
        return new FriendTagDTO(
                entity.getId(),
                entity.getDisplayName(),
                entity.getColorHexCode(),
                ownerUserId,
                friendUserIds,
                entity.isEveryone()

        );
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        return new FriendTag(
                dto.getId(),
                dto.getDisplayName(),
                dto.getColorHexCode(),
                dto.getOwnerUserId(),
                dto.isEveryone()
        );
    }

    public static FriendTag toEntity(FriendTagCreationDTO dto) {
        return new FriendTag(
                dto.getId(),
                dto.getDisplayName(),
                dto.getColorHexCode(),
                dto.getOwnerUserId(),
                false
        );
    }

    public static List<FriendTagDTO> toDTOList(
            List<FriendTag> entities,
            Map<FriendTag, UUID> ownerUserIdsMap,
            Map<FriendTag, List<UUID>> friendUserIdsMap
    ) {
        return entities.stream()
                .map(friendTag -> toDTO(
                        friendTag,
                        ownerUserIdsMap.getOrDefault(friendTag, null), // Default to null if owner is missing
                        friendUserIdsMap.getOrDefault(friendTag, List.of()) // Default to an empty list if friends are missing
                ))
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> friendTagDTOs) {
        return friendTagDTOs.stream()
                .map(FriendTagMapper::toEntity)
                .collect(Collectors.toList());
    }
}
