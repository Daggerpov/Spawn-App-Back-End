package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.BlockedUserDTO;
import com.danielagapov.spawn.Models.BlockedUser;
import com.danielagapov.spawn.Models.User;

public class BlockedUserMapper {

    public static BlockedUser toEntity(BlockedUserDTO dto, User blocker, User blocked) {
        BlockedUser entity = new BlockedUser();
        entity.setId(dto.getId());
        entity.setBlocker(blocker);
        entity.setBlocked(blocked);
        entity.setReason(dto.getReason());
        return entity;
    }

    public static BlockedUserDTO toDTO(BlockedUser entity) {
        return new BlockedUserDTO(
                entity.getId(),
                entity.getBlocker().getId(),
                entity.getBlocked().getId(),
                entity.getBlocker().getUsername(),
                entity.getBlocked().getUsername(),
                entity.getReason()
        );
    }
}
