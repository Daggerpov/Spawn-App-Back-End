package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService implements IEventService {
    private final IEventRepository repository;
    private final ILocationRepository locationRepository;
    private final IFriendTagService friendTagService;

    public EventService(IEventRepository repository, ILocationRepository locationRepository, IFriendTagService friendTagService) {
        this.repository = repository;
        this.locationRepository = locationRepository;
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
                throw new BasesNotFoundException(EntityType.User);
            }

            // Step 2: Collect all friend user IDs
            List<UUID> friendIds = friends.stream()
                    .map(UserDTO::id)
                    .toList();

            // Step 3: Filter events based on whether their owner is in the list of friend IDs
            List<Event> allEvents = repository.findAll();
            List<Event> filteredEvents = allEvents.stream()
                    .filter(event -> friendIds.contains(event.getCreator().getId()))
                    .collect(Collectors.toList());

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

            // Map EventDTO to Event entity
            Event eventEntity = EventMapper.toEntity(event, location);

            // Save Event entity
            repository.save(eventEntity);

            // Return the saved Event as a DTO
            return EventMapper.toDTO(eventEntity);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save event: " + e.getMessage());
        }
    }

    public List<EventDTO> getEventsByUserId(UUID userId) {
        List<Event> events = repository.findAll()
                .stream()
                .filter(event -> event.getCreator().getId().equals(userId))
                .collect(Collectors.toList());

        if (events.isEmpty()) {
            throw new BasesNotFoundException(EntityType.Event);
        }

        return EventMapper.toDTOList(events);
    }

    public EventDTO replaceEvent(EventDTO newEvent, UUID id) {
        return repository.findById(id).map(event -> {
            // Update existing event fields
            event.setTitle(newEvent.title());
            event.setNote(newEvent.note());
            event.setEndTime(newEvent.endTime());
            event.setStartTime(newEvent.startTime());

            // Convert LocationDTO to Location entity
            Location location = LocationMapper.toEntity(newEvent.location());

            // Save or resolve the new location
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
            // Handle case where event does not exist (create new)
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
}
