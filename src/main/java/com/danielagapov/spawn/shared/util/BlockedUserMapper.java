package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.user.api.dto.BlockedUserDTO;
import com.danielagapov.spawn.user.internal.domain.BlockedUser;
import com.danielagapov.spawn.user.internal.domain.User;

public final class BlockedUserMapper {

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
