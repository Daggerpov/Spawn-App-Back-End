package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventService implements IEventService {
    private final IEventRepository repository;
    private final ILocationRepository locationRepository;
    private final IEventUserRepository eventUserRepository;
    private final IFriendTagService friendTagService;

    public EventService(IEventRepository repository, ILocationRepository locationRepository,
            IEventUserRepository eventUserRepository, IFriendTagService friendTagService) {
        this.repository = repository;
        this.locationRepository = locationRepository;
        this.eventUserRepository = eventUserRepository;
        this.friendTagService = friendTagService;
    }

    public List<EventDTO> getAllEvents() {
        try {
            return EventMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            throw new BasesNotFoundException(EntityType.Event);
        }
    }

    public EventDTO getEventById(UUID id) {
        return EventMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, id)));
    }

    public List<EventDTO> getEventsByFriendTagId(UUID tagId) {
        try {
            // Step 1: Get the FriendTag and associated friends
            FriendTagDTO friendTag = friendTagService.getFriendTagById(tagId);
            List<UserDTO> friends = friendTag.friends();

            if (friends.isEmpty()) {
                return List.of();
            }

            // Step 2: Collect all friend user IDs
            List<UUID> friendIds = friends.stream()
                    .map(UserDTO::id)
                    .toList();

            // Step 3: Filter events based on whether their owner is in the list of friend
            // IDs
            List<Event> filteredEvents = repository.findByCreatorIdIn(friendIds);

            if (filteredEvents.isEmpty()) {
                throw new BasesNotFoundException(EntityType.Event);
            }

            // Step 4: Map filtered events to DTOs
            return EventMapper.toDTOList(filteredEvents);
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving events by tag ID", e);
        }
    }

    public EventDTO saveEvent(EventDTO event) {
        try {
            // Convert LocationDTO to Location entity
            Location location = LocationMapper.toEntity(event.location());

            // Save or resolve location from repository
            if (location.getId() != null) {
                location = locationRepository.findById(location.getId())
                        .orElse(locationRepository.save(location));
            } else {
                location = locationRepository.save(location);
            }

            Event eventEntity = EventMapper.toEntity(event, location);
            repository.save(eventEntity);
            return EventMapper.toDTO(eventEntity);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save event: " + e.getMessage());
        }
    }

    public List<EventDTO> getEventsByUserId(UUID userId) {
        List<Event> events = repository.findByCreatorId(userId);

        if (events.isEmpty()) {
            throw new BasesNotFoundException(EntityType.Event);
        }

        return EventMapper.toDTOList(events);
    }

    public EventDTO replaceEvent(EventDTO newEvent, UUID id) {
        return repository.findById(id).map(event -> {
            event.setTitle(newEvent.title());
            event.setNote(newEvent.note());
            event.setEndTime(newEvent.endTime());
            event.setStartTime(newEvent.startTime());

            Location location = LocationMapper.toEntity(newEvent.location());

            if (location.getId() != null) {
                location = locationRepository.findById(location.getId())
                        .orElse(locationRepository.save(location));
            } else {
                location = locationRepository.save(location);
            }

            event.setLocation(location);
            repository.save(event);
            return EventMapper.toDTO(event);
        }).orElseGet(() -> {
            Location location = LocationMapper.toEntity(newEvent.location());
            if (location.getId() != null) {
                location = locationRepository.findById(location.getId())
                        .orElse(locationRepository.save(location));
            } else {
                location = locationRepository.save(location);
            }

            Event eventEntity = EventMapper.toEntity(newEvent, location);
            repository.save(eventEntity);
            return EventMapper.toDTO(eventEntity);
        });
    }

    public boolean deleteEventById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.Event, id);
        }

        try {
            repository.deleteById(id);
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }

    public List<UserDTO> getParticipatingUsersByEventId(UUID id) {
        return List.of();
    }

    // TODO: optimize this
    public ParticipationStatus getParticipationStatus(UUID eventId, UUID userId) {
        if (!eventUserRepository.existsById(eventId)) {
            throw new BaseNotFoundException(EntityType.Event, eventId);
        }

        try {
            List<EventDTO> events = getAllEvents();
            for (int i = 0; i < events.size(); i++) {
                if (events.get(i).id().equals(eventId)) {
                    List<UserDTO> invited = events.get(i).invited();
                    List<UserDTO> participants = events.get(i).participants();
                    for (int j = 0; j < invited.size(); i++) {
                        if (invited.get(j).id().equals(userId)) {
                            return ParticipationStatus.invited;
                        }
                    }

                    for (int j = 0; j < participants.size(); i++) {
                        if (participants.get(j).id().equals(userId)) {
                            return ParticipationStatus.participating;
                        }
                    }

                    return ParticipationStatus.notInvited;
                }
                ;
            }
        } catch (DataAccessException e) {
            return ParticipationStatus.notInvited;
        }

        return ParticipationStatus.invited;
    }

    public void inviteUser(UUID eventId, UUID userId) {
        // TODO: take an eventId and userId and give the status of invited to that user
        // to that event
        // will need to start thinking about utilizing EventParticipationDTO and a
        // mapper for it
    }

    public boolean toggleParticipation(UUID eventId, UUID userId) {
        // TODO: best to do this after implementing EventUser table and DTO fully
        // returns true after either setting an invited to a participants or vice versa
        // otherwise returns false
        return false;
    }
}
