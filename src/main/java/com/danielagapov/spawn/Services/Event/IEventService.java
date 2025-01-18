package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.util.List;
import java.util.UUID;

public interface IEventService {
    public List<EventDTO> getAllEvents();
    public EventDTO getEventById(UUID id);
    public List<EventDTO> getEventsByFriendTagId(UUID friendTagId);
    public EventDTO saveEvent(EventDTO event);
    public List<EventDTO> getEventsByUserId(UUID userId);
    public EventDTO replaceEvent(EventDTO event, UUID eventId);
    public boolean deleteEventById(UUID id);
    public List<UserDTO> getParticipatingUsersByEventId(UUID id);
    public ParticipationStatus getParticipationStatus(UUID eventId, UUID userId);
    public void inviteUser(UUID eventId, UUID userId);
    public boolean toggleParticipation(UUID eventId, UUID userId);
}
