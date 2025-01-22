package com.danielagapov.spawn.DTOs;


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
        UserDTO creatorUser,
        List<UserDTO> participantUsers,
        List<UserDTO> invitedUsers,
        List<ChatMessageDTO> chatMessages,
        /// useful for event retrieval from a user's feed/map view on mobile:
        String eventFriendTagColorHexCodeForRequestingUser
) implements Serializable, IEventDTO {}