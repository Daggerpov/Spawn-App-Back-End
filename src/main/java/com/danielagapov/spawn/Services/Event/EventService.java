package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
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

    public List<Event> getAllEvents() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new BasesNotFoundException();
        }
    }

    public Event getEventById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id));
    }

    public List<Event> getEventsByTagId(Long tagId) {
        // TODO: change this logic later, once tags are setup.
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving events", e);
        }
    }

    public Event saveEvent(Event event) {
        try {
            return repository.save(event);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save event: " + e.getMessage());
        }
    }
}
