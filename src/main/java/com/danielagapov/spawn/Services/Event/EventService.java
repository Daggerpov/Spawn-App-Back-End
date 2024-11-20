package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Models.Event.Event;
import com.danielagapov.spawn.Repositories.IEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventService implements IEventService {
    private final IEventRepository repository;

    @Autowired
    public EventService(IEventRepository repository) {
        this.repository = repository;
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
        // TODO: change this logic later, once tags are setup.
        try {
            return EventMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving events", e);
        }
    }

    public EventDTO saveEvent(EventDTO event) {
        try {
            Event eventEntity = EventMapper.toEntity(event);
            repository.save(eventEntity);
            return EventMapper.toDTO(eventEntity);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save event: " + e.getMessage());
        }
    }

    public List<EventDTO> getEventsByUserId(UUID userId) {
        return EventMapper.toDTOList(repository.findAll());
    }

    // basically 'upserting' (a.k.a. inserting if not already in DB, otherwise, updating)
    public EventDTO replaceEvent(EventDTO newEvent, UUID id) {
        // TODO: we may want to make this function easier to read in the future,
        // but for now, I left the logic the same as what Seabert wrote.
        return repository.findById(id).map(event -> {
            // Update the existing event's details with the new data from the DTO
            event.setTitle(newEvent.title());
            event.setNote(newEvent.note());
            event.setEndTime(newEvent.endTime());
            event.setLocation(newEvent.location());
            event.setStartTime(newEvent.startTime());

            repository.save(event);
            return EventMapper.toDTO(event);
        }).orElseGet(() -> {
            // If the event doesn't exist, create a new event and save it
            Event eventEntity = EventMapper.toEntity(newEvent); // Create a new event from DTO
            repository.save(eventEntity);
            return EventMapper.toDTO(eventEntity);
        });
    }
}
