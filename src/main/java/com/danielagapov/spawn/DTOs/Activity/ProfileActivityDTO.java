package com.danielagapov.spawn.DTOs.Activity;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO specifically for profile view activities that includes whether the activity is past or upcoming
 */
@Getter
@Setter
public class ProfileActivityDTO extends AbstractActivityDTO {
    private UUID locationId;
    private BaseUserDTO creatorUser;
    private List<BaseUserDTO> participantUsers;
    private List<BaseUserDTO> invitedUsers;
    private List<UUID> chatMessageIds;
    
    public ProfileActivityDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    UUID locationId,
    String note,
    String icon,
    BaseUserDTO creatorUser,
    List<BaseUserDTO> participantUsers,
    List<BaseUserDTO> invitedUsers,
    List<UUID> chatMessageIds,
    Instant createdAt) {
        super(id, title, startTime, endTime, note, icon, createdAt);
        this.locationId = locationId;
        this.creatorUser = creatorUser;
        this.participantUsers = participantUsers;
        this.invitedUsers = invitedUsers;
        this.chatMessageIds = chatMessageIds;
    }
    
    /**
     * Creates a ProfileActivityDTO from a FullFeedActivityDTO
     * 
     * @param fullFeedActivityDTO The FullFeedActivityDTO to convert
     * @return A new ProfileActivityDTO
     */
    public static ProfileActivityDTO fromFullFeedActivityDTO(FullFeedActivityDTO fullFeedActivityDTO) {
        // Convert chat messages to their IDs
        List<UUID> chatMessageIds = fullFeedActivityDTO.getChatMessages() != null ? 
            fullFeedActivityDTO.getChatMessages().stream().map(msg -> msg.getId()).collect(java.util.stream.Collectors.toList()) : 
            null;
            
        return new ProfileActivityDTO(
            fullFeedActivityDTO.getId(),
            fullFeedActivityDTO.getTitle(),
            fullFeedActivityDTO.getStartTime(),
            fullFeedActivityDTO.getEndTime(),
            fullFeedActivityDTO.getLocation().getId(),
            fullFeedActivityDTO.getNote(),
            fullFeedActivityDTO.getIcon(),
            fullFeedActivityDTO.getCreatorUser(),
            fullFeedActivityDTO.getParticipantUsers(),
            fullFeedActivityDTO.getInvitedUsers(),
            chatMessageIds,
            fullFeedActivityDTO.getCreatedAt()
        );
    }
} 