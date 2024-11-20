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

    public EventDTO getEventById(Long id) {
        return EventMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id)));
    }

    public List<EventDTO> getEventsByTagId(Long tagId) {
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

    public List<EventDTO> getEventsByUserId(Long userId) {
        return EventMapper.toDTOList(repository.findAll());
    }

    public EventDTO replaceEvent(EventDTO newEvent, Long id) {
        Event eventEntity = EventMapper.toEntity(newEvent);

        return repository.findById(id).map(event -> {
            event.setTitle(eventEntity.getTitle());
            event.setNote(eventEntity.getNote());
            event.setEndTime(eventEntity.getEndTime());
            event.setLocation(eventEntity.getLocation());
            event.setStartTime(eventEntity.getStartTime());

            repository.save(event);
            return EventMapper.toDTO(eventEntity);
        }).orElseGet(() -> {
            repository.save(eventEntity);
            return EventMapper.toDTO(eventEntity);
        });
    }
}
