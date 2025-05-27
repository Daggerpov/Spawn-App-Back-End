package com.danielagapov.spawn.DTOs.Event;

import com.danielagapov.spawn.DTOs.ChatMessage.FullEventChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.EventCategory;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO specifically for profile view events that includes whether the event is past or upcoming
 */
@Getter
@Setter
public class ProfileEventDTO extends FullFeedEventDTO {
    @JsonProperty("isPastEvent")
    private boolean isPastEvent;

    public ProfileEventDTO(UUID id,
                          String title,
                          OffsetDateTime startTime,
                          OffsetDateTime endTime,
                          LocationDTO location,
                          String note,
                          String icon,
                          EventCategory category,
                          BaseUserDTO creatorUser,
                          List<BaseUserDTO> participantUsers,
                          List<BaseUserDTO> invitedUsers,
                          List<FullEventChatMessageDTO> chatMessages,
                          String eventFriendTagColorHexCodeForRequestingUser,
                          ParticipationStatus participationStatus,
                          boolean isSelfOwned,
                          boolean isPastEvent,
                          Instant createdAt) {
        super(id, title, startTime, endTime, location, note, icon, category, creatorUser, 
             participantUsers, invitedUsers, chatMessages, eventFriendTagColorHexCodeForRequestingUser, 
             participationStatus, isSelfOwned, createdAt);
        this.isPastEvent = isPastEvent;
    }
    
    /**
     * Creates a ProfileEventDTO from a FullFeedEventDTO
     * 
     * @param fullFeedEventDTO The FullFeedEventDTO to convert
     * @param isPastEvent Whether this event is a past event
     * @return A new ProfileEventDTO
     */
    public static ProfileEventDTO fromFullFeedEventDTO(FullFeedEventDTO fullFeedEventDTO, boolean isPastEvent) {
        return new ProfileEventDTO(
            fullFeedEventDTO.getId(),
            fullFeedEventDTO.getTitle(),
            fullFeedEventDTO.getStartTime(),
            fullFeedEventDTO.getEndTime(),
            fullFeedEventDTO.getLocation(),
            fullFeedEventDTO.getNote(),
            fullFeedEventDTO.getIcon(),
            fullFeedEventDTO.getCategory(),
            fullFeedEventDTO.getCreatorUser(),
            fullFeedEventDTO.getParticipantUsers(),
            fullFeedEventDTO.getInvitedUsers(),
            fullFeedEventDTO.getChatMessages(),
            fullFeedEventDTO.getEventFriendTagColorHexCodeForRequestingUser(),
            fullFeedEventDTO.getParticipationStatus(),
            fullFeedEventDTO.isSelfOwned(),
            isPastEvent,
            fullFeedEventDTO.getCreatedAt()
        );
    }
} 