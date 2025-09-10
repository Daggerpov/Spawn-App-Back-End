package com.danielagapov.spawn.DTOs.Activity;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO specifically for profile view activities that includes whether the activity is past or upcoming
 */
@NoArgsConstructor
@Getter
@Setter
public class ProfileActivityDTO extends AbstractActivityDTO {
    private LocationDTO location;
    private BaseUserDTO creatorUser;
    private List<BaseUserDTO> participantUsers;
    private List<BaseUserDTO> invitedUsers;
    private List<UUID> chatMessageIds;
    
    public ProfileActivityDTO(UUID id,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    LocationDTO location,
    String note,
    String icon,
    Integer participantLimit,
    BaseUserDTO creatorUser,
    List<BaseUserDTO> participantUsers,
    List<BaseUserDTO> invitedUsers,
    List<UUID> chatMessageIds,
    Instant createdAt,
    boolean isExpired,
    String clientTimezone) {
        super(id, title, startTime, endTime, note, icon, participantLimit, createdAt, isExpired, clientTimezone);
        this.location = location;
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
            fullFeedActivityDTO.getLocation(),
            fullFeedActivityDTO.getNote(),
            fullFeedActivityDTO.getIcon(),
            fullFeedActivityDTO.getParticipantLimit(),
            fullFeedActivityDTO.getCreatorUser(),
            fullFeedActivityDTO.getParticipantUsers(),
            fullFeedActivityDTO.getInvitedUsers(),
            chatMessageIds,
            fullFeedActivityDTO.getCreatedAt(),
            fullFeedActivityDTO.isExpired(),
            fullFeedActivityDTO.getClientTimezone()
        );
    }
} 