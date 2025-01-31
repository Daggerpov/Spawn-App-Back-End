package com.danielagapov.spawn.DTOs;


import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FullFeedEventDTO(
        UUID id,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        LocationDTO location,
        String note,
        FullUserDTO creatorUser,
        List<FullUserDTO> participantUsers,
        List<FullUserDTO> invitedUsers,
        List<FullEventChatMessageDTO> chatMessages,
        /// useful for event retrieval from a user's feed/map view on mobile:
        String eventFriendTagColorHexCodeForRequestingUser,
        // ensures string formatting when serialized to JSON, for mobile (client)
        @JsonFormat(shape = JsonFormat.Shape.STRING) ParticipationStatus participationStatus
) implements Serializable, IEventDTO {}