package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserDTO;
import com.danielagapov.spawn.Models.User.BlockedUser;
import com.danielagapov.spawn.Models.User.User;

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
        if (entity == null) {
            throw new IllegalArgumentException("BlockedUser entity cannot be null");
        }
        
        if (entity.getBlocker() == null) {
            throw new IllegalArgumentException("Blocker user cannot be null for BlockedUser with id: " + entity.getId());
        }
        
        if (entity.getBlocked() == null) {
            throw new IllegalArgumentException("Blocked user cannot be null for BlockedUser with id: " + entity.getId());
        }
        
        return new BlockedUserDTO(
                entity.getId(),
                entity.getBlocker().getId(),
                entity.getBlocked().getId(),
                entity.getBlocker().getUsername() != null ? entity.getBlocker().getUsername() : "",
                entity.getBlocked().getUsername() != null ? entity.getBlocked().getUsername() : "",
                entity.getBlocked().getName() != null ? entity.getBlocked().getName() : "",
                entity.getBlocked().getProfilePictureUrlString() != null ? entity.getBlocked().getProfilePictureUrlString() : "",
                entity.getReason() != null ? entity.getReason() : ""
        );
    }
}
