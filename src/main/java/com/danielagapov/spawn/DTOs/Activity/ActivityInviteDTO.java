package com.danielagapov.spawn.DTOs.Activity;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for external activity invites - contains only essential information
 * needed for the activity invite page without requiring authentication
 */
@Getter
@Setter
public class ActivityInviteDTO extends AbstractActivityDTO {
    UUID locationId;
    UUID creatorUserId;
    List<UUID> participantUserIds;
    List<UUID> invitedUserIds;
    
    public ActivityInviteDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    UUID locationId,
    String note,
    String icon,
    UUID creatorUserId,
    List<UUID> participantUserIds,
    List<UUID> invitedUserIds,
    Instant createdAt) {
        super(id, title, startTime, endTime, note, icon, createdAt);
        this.locationId = locationId;
        this.creatorUserId = creatorUserId;
        this.participantUserIds = participantUserIds;
        this.invitedUserIds = invitedUserIds;
    }
} 