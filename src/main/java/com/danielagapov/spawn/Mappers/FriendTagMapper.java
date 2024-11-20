package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Models.FriendTag.FriendTag;

public class FriendTagMapper {

    public static FriendTagDTO toDTO(FriendTag entity) {
        return new FriendTagDTO(
                entity.getId(),
                entity.getDisplayName(),
                entity.getColor(),
                // TODO: do later once proper relationships in entity classes are setup:
//                UserMapper.toDTO(entity.getOwner())
                null
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        FriendTag friendTag = new FriendTag();
        friendTag.setId(dto.id());
        friendTag.setDisplayName(dto.displayName());
        friendTag.setColor(dto.color());
        // TODO: do later once proper relationships in entity classes are setup:
//        friendTag.setOwner(UserMapper.toEntity(dto.owner())); // Map UserDTO to User entity
        return friendTag;
    }
}
