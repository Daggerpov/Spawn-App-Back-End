package com.danielagapov.spawn.DTOs.Activity;


import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFeedActivityDTO extends AbstractActivityDTO {
    private LocationDTO location;
    private UUID activityTypeId;
    private BaseUserDTO creatorUser;
    private List<BaseUserDTO> participantUsers;
    private List<BaseUserDTO> invitedUsers;
    private List<FullActivityChatMessageDTO> chatMessages;
    /// useful for activity retrieval from a user's feed/map view on mobile:
    private String activityFriendTagColorHexCodeForRequestingUser;
    // ensures string formatting when serialized to JSON; for mobile (client)
    private @JsonFormat(shape = JsonFormat.Shape.STRING) ParticipationStatus participationStatus;
    @JsonProperty("isSelfOwned") // specifying JSON name,
    // since booleans get turned to `selfOwned` (remove `is` from name)
    private boolean isSelfOwned;

    public FullFeedActivityDTO(UUID id,
                            String title,
                            OffsetDateTime startTime,
                            OffsetDateTime endTime,
                            LocationDTO location,
                            UUID activityTypeId,
                            String note,
                            String icon,
                            BaseUserDTO creatorUser,
                            List<BaseUserDTO> participantUsers,
                            List<BaseUserDTO> invitedUsers,
                            List<FullActivityChatMessageDTO> chatMessages,
                            String activityFriendTagColorHexCodeForRequestingUser,
                            ParticipationStatus participationStatus, 
                            boolean isSelfOwned,
                            Instant createdAt) {
        super(id, title, startTime, endTime, note, icon, createdAt);
        this.location = location;
        this.activityTypeId = activityTypeId;
        this.creatorUser = creatorUser;
        this.participantUsers = participantUsers;
        this.invitedUsers = invitedUsers;
        this.chatMessages = chatMessages;
        this.activityFriendTagColorHexCodeForRequestingUser = activityFriendTagColorHexCodeForRequestingUser;
        this.participationStatus = participationStatus;
        this.isSelfOwned = isSelfOwned;
    }
}