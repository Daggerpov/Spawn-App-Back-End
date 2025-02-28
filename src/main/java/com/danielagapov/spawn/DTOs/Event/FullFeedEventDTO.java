package com.danielagapov.spawn.DTOs.Event;


import com.danielagapov.spawn.DTOs.ChatMessage.FullEventChatMessageDTO;
import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.DTOs.User.FullUserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FullFeedEventDTO extends AbstractEventDTO implements Serializable {
    private LocationDTO location;
    private FullUserDTO creatorUser;
    private List<FullUserDTO> participantUsers;
    private List<FullUserDTO> invitedUsers;
    private List<FullEventChatMessageDTO> chatMessages;
    /// useful for event retrieval from a user's feed/map view on mobile:
    private String eventFriendTagColorHexCodeForRequestingUser;
    // ensures string formatting when serialized to JSON; for mobile (client)
    private @JsonFormat(shape = JsonFormat.Shape.STRING) ParticipationStatus participationStatus;

    public FullFeedEventDTO(UUID id,
                        String title,
                        OffsetDateTime startTime,
                        OffsetDateTime endTime,
                        LocationDTO location,
                        String note,
                        FullUserDTO creatorUser,
                        List<FullUserDTO> participantUsers,
                        List<FullUserDTO> invitedUsers,
                        List<FullEventChatMessageDTO> chatMessages,
                            String eventFriendTagColorHexCodeForRequestingUser,
                            ParticipationStatus participationStatus,
                            boolean isSelfOwned) {
        super(id, title, startTime, endTime, note);
        this.location = location;
        this.creatorUser = creatorUser;
        this.participantUsers = participantUsers;
        this.invitedUsers = invitedUsers;
        this.chatMessages = chatMessages;
        this.eventFriendTagColorHexCodeForRequestingUser = eventFriendTagColorHexCodeForRequestingUser;
        this.participationStatus = participationStatus;
    }
}