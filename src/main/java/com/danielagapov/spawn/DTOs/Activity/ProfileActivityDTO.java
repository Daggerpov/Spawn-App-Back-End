package com.danielagapov.spawn.DTOs.Activity;

import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.ActivityCategory;
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
public class ProfileActivityDTO extends FullFeedActivityDTO {
    @JsonProperty("isPastActivity")
    private boolean isPastActivity;

    public ProfileActivityDTO(UUID id,
                          String title,
                          OffsetDateTime startTime,
                          OffsetDateTime endTime,
                          LocationDTO location,
                          String note,
                          String icon,
                          ActivityCategory category,
                          BaseUserDTO creatorUser,
                          List<BaseUserDTO> participantUsers,
                          List<BaseUserDTO> invitedUsers,
                          List<FullActivityChatMessageDTO> chatMessages,
                          String activityFriendTagColorHexCodeForRequestingUser,
                          ParticipationStatus participationStatus,
                          boolean isSelfOwned,
                          boolean isPastActivity,
                          Instant createdAt,
                          Boolean isIndefinite) {
        super(id, title, startTime, endTime, location, note, icon, category, creatorUser, 
             participantUsers, invitedUsers, chatMessages, activityFriendTagColorHexCodeForRequestingUser, 
             participationStatus, isSelfOwned, createdAt, isIndefinite);
        this.isPastActivity = isPastActivity;
    }
    
    /**
     * Creates a ProfileActivityDTO from a FullFeedActivityDTO
     * 
     * @param fullFeedActivityDTO The FullFeedActivityDTO to convert
     * @param isPastActivity Whether this activity is a past activity
     * @return A new ProfileActivityDTO
     */
    public static ProfileActivityDTO fromFullFeedActivityDTO(FullFeedActivityDTO fullFeedActivityDTO, boolean isPastActivity) {
        return new ProfileActivityDTO(
            fullFeedActivityDTO.getId(),
            fullFeedActivityDTO.getTitle(),
            fullFeedActivityDTO.getStartTime(),
            fullFeedActivityDTO.getEndTime(),
            fullFeedActivityDTO.getLocation(),
            fullFeedActivityDTO.getNote(),
            fullFeedActivityDTO.getIcon(),
            fullFeedActivityDTO.getCategory(),
            fullFeedActivityDTO.getCreatorUser(),
            fullFeedActivityDTO.getParticipantUsers(),
            fullFeedActivityDTO.getInvitedUsers(),
            fullFeedActivityDTO.getChatMessages(),
            fullFeedActivityDTO.getActivityFriendTagColorHexCodeForRequestingUser(),
            fullFeedActivityDTO.getParticipationStatus(),
            fullFeedActivityDTO.isSelfOwned(),
            isPastActivity,
            fullFeedActivityDTO.getCreatedAt(),
            fullFeedActivityDTO.getIsIndefinite()
        );
    }
} 