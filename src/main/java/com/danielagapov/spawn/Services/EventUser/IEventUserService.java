package com.danielagapov.spawn.Services.EventUser;

import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.util.List;
import java.util.UUID;

public interface IEventUserService {

    boolean inviteUser(UUID eventId, UUID userId);

    List<UserDTO> getParticipatingUsersByEventId(UUID eventId);

    List<BaseUserDTO> getParticipantsByEventId(UUID eventId);

    List<BaseUserDTO> getInvitedByEventId(UUID eventId);

    List<UUID> getParticipantUserIdsByEventId(UUID eventId);

    List<UUID> getInvitedUserIdsByEventId(UUID eventId);

    ParticipationStatus getParticipationStatus(UUID eventId, UUID userId);

    // returns back the updated event dto, with participants and invited users updated:
    FullFeedEventDTO toggleParticipation(UUID eventId, UUID userId);

    List<EventDTO> getEventsInvitedTo(UUID id);

}
