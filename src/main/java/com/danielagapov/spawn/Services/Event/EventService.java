package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Event.Event;
import com.danielagapov.spawn.Models.Location.Location;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Repositories.IEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventService implements IEventService {
    private final IEventRepository repository;
    private final ILocationRepository locationRepository;

    @Autowired
    public EventService(IEventRepository repository, ILocationRepository locationRepository) {
        this.repository = repository;
        this.locationRepository = locationRepository;
    }

    public List<EventDTO> getAllEvents() {
        try {
            return EventMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            throw new BasesNotFoundException();
        }
    }

    public EventDTO getEventById(UUID id) {
        return EventMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id)));
    }

    public List<EventDTO> getEventsByTagId(UUID tagId) {
        // TODO: Implement proper logic once tag relationships are configured
        try {
            return EventMapper.toDTOList(repository.findAll());
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
        // TODO: Add proper filtering logic for events by user
        return EventMapper.toDTOList(repository.findAll());
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
}
