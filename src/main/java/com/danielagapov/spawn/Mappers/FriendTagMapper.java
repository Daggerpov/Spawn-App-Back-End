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
                entity.getColor(),
                // TODO: setup later once proper relationships in entity classes are setup:
                null
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        FriendTag friendTag = new FriendTag();
        friendTag.setId(dto.id());
        friendTag.setDisplayName(dto.displayName());
        friendTag.setColor(dto.colorHexCode());
        // TODO: setup later once proper relationships in entity classes are setup:
        return friendTag;
    }

    public static List<FriendTagDTO> toDTOList(List<FriendTag> entities) {
        return entities.stream()
                .map(FriendTagMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> dtos) {
        return dtos.stream()
                .map(FriendTagMapper::toEntity)
                .collect(Collectors.toList());
    }
}
