package com.danielagapov.spawn;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Services.Event.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EventServiceTests {

    @Mock
    private IEventRepository eventRepository;

    @Mock
    private ILocationRepository locationRepository;

    @InjectMocks
    private EventService eventService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    void getAllEvents_ShouldReturnList_WhenEventsExist() {
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        Event event = new Event(UUID.randomUUID(), "Test Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), location, "Test note", null);

        when(eventRepository.findAll()).thenReturn(List.of(event));

        List<EventDTO> result = eventService.getAllEvents();

        assertFalse(result.isEmpty());
        assertEquals("Test Event", result.get(0).title());
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void getAllEvents_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(eventRepository.findAll()).thenThrow(new DataAccessException("Database error") {});

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> eventService.getAllEvents());

        assertTrue(exception.getMessage().contains("Error fetching events"));
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void getEventById_ShouldReturnEvent_WhenEventExists() {
        UUID eventId = UUID.randomUUID();
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        Event event = new Event(eventId, "Test Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), location, "Test note", null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        EventDTO result = eventService.getEventById(eventId);

        assertEquals("Test Event", result.title());
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void getEventById_ShouldThrowException_WhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> eventService.getEventById(eventId));

        assertEquals("Entity not found with ID: " + eventId, exception.getMessage());
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void saveEvent_ShouldSaveEvent_WhenValidData() {
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Park", 40.7128, -74.0060);
        EventDTO eventDTO = new EventDTO(UUID.randomUUID(), "Birthday Party", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2), LocationMapper.toDTO(location), "Bring your own snacks!", null, List.of(), List.of(), List.of());

        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(eventRepository.save(any(Event.class))).thenReturn(EventMapper.toEntity(eventDTO, location));

        assertDoesNotThrow(() -> eventService.saveEvent(eventDTO));

        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void saveEvent_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Park", 40.7128, -74.0060);
        EventDTO eventDTO = new EventDTO(UUID.randomUUID(), "Birthday Party", OffsetDateTime.now(), OffsetDateTime.now().plusHours(2), LocationMapper.toDTO(location), "Bring your own snacks!", null, List.of(), List.of(), List.of());

        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(eventRepository.save(any(Event.class))).thenThrow(new DataAccessException("Database error") {});

        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> eventService.saveEvent(eventDTO));

        assertTrue(exception.getMessage().contains("Failed to save event"));
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void deleteEventById_ShouldDeleteEvent_WhenEventExists() {
        UUID eventId = UUID.randomUUID();

        when(eventRepository.existsById(eventId)).thenReturn(true);

        assertDoesNotThrow(() -> eventService.deleteEventById(eventId));

        verify(eventRepository, times(1)).deleteById(eventId);
    }

    @Test
    void deleteEventById_ShouldThrowException_WhenEventNotFound() {
        UUID eventId = UUID.randomUUID();

        when(eventRepository.existsById(eventId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> eventService.deleteEventById(eventId));

        assertEquals("Entity not found with ID: " + eventId, exception.getMessage());
        verify(eventRepository, never()).deleteById(eventId);
    }

    @Test
    void deleteEventById_ShouldReturnFalse_WhenDatabaseErrorOccurs() {
        UUID eventId = UUID.randomUUID();

        when(eventRepository.existsById(eventId)).thenReturn(true);
        doThrow(new DataAccessException("Database error") {}).when(eventRepository).deleteById(eventId);

        boolean result = eventService.deleteEventById(eventId);

        assertFalse(result);
        verify(eventRepository, times(1)).deleteById(eventId);
    }
}
