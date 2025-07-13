package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTag.AbstractFriendTagDTO;
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

    public static FriendTag toEntity(AbstractFriendTagDTO dto) {
        if (dto instanceof FriendTagDTO) {
            return toEntity((FriendTagDTO) dto);
        } else {
            throw new IllegalArgumentException("Unsupported DTO type: " + dto.getClass().getSimpleName());
        }
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        return new FriendTag(
                dto.getId(),
                dto.getDisplayName(),
                dto.getColorHexCode(),
                dto.getOwnerUserId(),
                dto.isEveryone(),
                null // lastModified will be set in the service
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
}
