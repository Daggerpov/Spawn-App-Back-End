package com.danielagapov.spawn.DTOs;


import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FullFeedEventDTO implements Serializable, IEventDTO {
    private UUID id;
    private String title;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private LocationDTO location;
    private String note;
    private FullUserDTO creatorUser;
    private List<FullUserDTO> participantUsers;
    private List<FullUserDTO> invitedUsers;
    private List<FullEventChatMessageDTO> chatMessages;
    /// useful for event retrieval from a user's feed/map view on mobile:
    private String eventFriendTagColorHexCodeForRequestingUser;
    // ensures string formatting when serialized to JSON; for mobile (client)
    private @JsonFormat(shape = JsonFormat.Shape.STRING) ParticipationStatus participationStatus;
}