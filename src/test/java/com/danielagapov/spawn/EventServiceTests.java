package com.danielagapov.spawn;

import com.danielagapov.spawn.DTOs.EventCreationDTO;
import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.Event.EventService;
import com.danielagapov.spawn.Services.Location.ILocationService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventServiceTests {

    @Mock
    private IEventRepository eventRepository;

    @Mock
    private ILogger logger;
    @Mock
    private ILocationRepository locationRepository;
    @Mock
    private ILocationService locationService;
    @Mock
    private IUserRepository userRepository;
    @Mock
    private IEventUserRepository eventUserRepository;

    @Mock
    private IUserService userService;

    @Mock
    private IChatMessageService chatMessageService;

    @InjectMocks
    private EventService eventService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    void getAllEvents_ShouldReturnList_WhenEventsExist() {
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        Event event = new Event(UUID.randomUUID(), "Test Event", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1), location, "Test note", null);

        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(userService.getParticipantUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByEventId(any(UUID.class))).thenReturn(List.of());

        List<EventDTO> result = eventService.getAllEvents();

        assertFalse(result.isEmpty());
        assertEquals("Test Event", result.get(0).title());
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void getEventById_ShouldReturnEvent_WhenEventExists() {
        UUID eventId = UUID.randomUUID();
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        Event event = new Event(eventId, "Test Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                location, "Test note", null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(userService.getParticipantUserIdsByEventId(eventId)).thenReturn(List.of());
        when(userService.getInvitedUserIdsByEventId(eventId)).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByEventId(eventId)).thenReturn(List.of());

        EventDTO result = eventService.getEventById(eventId);

        assertEquals("Test Event", result.title());
        verify(eventRepository, times(1)).findById(eventId);
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
    void saveEvent_ShouldSaveEvent_WhenValidData() {
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Park", 40.7128, -74.0060);
        EventDTO eventDTO = new EventDTO(UUID.randomUUID(), "Birthday Party", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2), location.getId(), "Bring your own snacks!", UUID.randomUUID(),
                List.of(), List.of(), List.of());
        User creator = new User(
                UUID.randomUUID(),
                "username",
                "profilePicture",
                "first",
                "last",
                "bio",
                "email");

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(userService.getUserEntityById(eventDTO.creatorUserId())).thenReturn(creator);
        when(eventRepository.save(any(Event.class))).thenReturn(EventMapper.toEntity(eventDTO, location, creator));

        assertDoesNotThrow(() -> eventService.saveEvent(eventDTO));

        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void saveEvent_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Park", 40.7128, -74.0060);
        EventDTO eventDTO = new EventDTO(UUID.randomUUID(), "Birthday Party", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2), location.getId(), "Bring your own snacks!", UUID.randomUUID(),
                List.of(), List.of(), List.of());

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(eventRepository.save(any(Event.class))).thenThrow(new DataAccessException("Database error") {
        });

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
    void deleteEventById_ShouldReturnFalse_WhenDatabaseErrorOccurs() {
        UUID eventId = UUID.randomUUID();

        when(eventRepository.existsById(eventId)).thenReturn(true);
        doThrow(new DataAccessException("Database error") {
        }).when(eventRepository).deleteById(eventId);

        boolean result = eventService.deleteEventById(eventId);

        assertFalse(result);
        verify(eventRepository, times(1)).deleteById(eventId);
    }

    /**
     * Test 1: Successful event creation with valid location, creator, and invites.
     * The test simulates invites from both friend tags and explicit invites.
     */
    @Test
    void createEvent_Successful() {
        // Arrange
        UUID creatorId = UUID.randomUUID();
        UUID friendTagId = UUID.randomUUID();
        UUID explicitInviteId = UUID.randomUUID();
        UUID friendTagUserId = UUID.randomUUID();

        // Create a LocationDTO for event creation.
        LocationDTO locationDTO = new LocationDTO(null, "Test Location", 0.0, 0.0);

        EventCreationDTO creationDTO = new EventCreationDTO(
                null,
                "Test Event",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                locationDTO,
                "Test note",
                creatorId,
                List.of(friendTagId),
                List.of(explicitInviteId)
        );

        // Stub location creation using locationService.save.
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        when(locationService.save(any(Location.class))).thenReturn(location);

        // Stub creator lookup.
        User creator = new User();
        creator.setId(creatorId);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        // Stub eventRepository.save to simulate saving the event (assign new UUID).
        Event savedEvent = new Event();
        UUID eventId = UUID.randomUUID();
        savedEvent.setId(eventId);
        savedEvent.setTitle("Test Event");
        savedEvent.setStartTime(creationDTO.startTime());
        savedEvent.setEndTime(creationDTO.endTime());
        savedEvent.setLocation(location);
        savedEvent.setNote("Test note");
        savedEvent.setCreator(creator);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Stub friend tag lookup: when userService.getFriendUserIdsByFriendTagId is called, return friendTagUserId.
        when(userService.getFriendUserIdsByFriendTagId(friendTagId)).thenReturn(List.of(friendTagUserId));

        // Stub chatMessageService to return an empty list for the new event.
        when(chatMessageService.getChatMessageIdsByEventId(eventId)).thenReturn(List.of());

        // Stub userRepository for invited users (both friend tag and explicit).
        User friendTagUser = new User();
        friendTagUser.setId(friendTagUserId);
        when(userRepository.findById(friendTagUserId)).thenReturn(Optional.of(friendTagUser));

        User explicitInvitedUser = new User();
        explicitInvitedUser.setId(explicitInviteId);
        when(userRepository.findById(explicitInviteId)).thenReturn(Optional.of(explicitInvitedUser));

        // Stub userService.getInvitedUserIdsByEventId to return the merged invites.
        Set<UUID> expectedInvited = new HashSet<>(Arrays.asList(friendTagUserId, explicitInviteId));
        when(userService.getInvitedUserIdsByEventId(eventId)).thenReturn(new ArrayList<>(expectedInvited));

        // Act
        EventDTO eventDTO = (EventDTO) eventService.createEvent(creationDTO);

        // Assert
        assertNotNull(eventDTO);
        assertEquals("Test Event", eventDTO.title());
        assertEquals(expectedInvited, new HashSet<>(eventDTO.invitedUserIds()));

        ArgumentCaptor<EventUser> captor = ArgumentCaptor.forClass(EventUser.class);
        verify(eventUserRepository, times(expectedInvited.size())).save(captor.capture());
        List<EventUser> savedInvites = captor.getAllValues();
        Set<UUID> savedInviteIds = new HashSet<>();
        for (EventUser eu : savedInvites) {
            savedInviteIds.add(eu.getUser().getId());
            assertEquals(ParticipationStatus.invited, eu.getStatus());
            assertEquals(eventId, eu.getEvent().getId());
        }
        assertEquals(expectedInvited, savedInviteIds);
    }


    /**
     * Test 2: Event creation fails when the Location is not found.
     */
    @Test
    void createEvent_Fails_WhenLocationNotCreated() {
        // Arrange
        UUID creatorId = UUID.randomUUID();
        EventCreationDTO creationDTO = new EventCreationDTO(
                null,
                "Test Event",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                new LocationDTO(null, "Test Location", 0.0, 0.0),
                "Test note",
                creatorId,
                List.of(),
                List.of()
        );

        // Stub locationService.save to simulate failure (throw an exception).
        when(locationService.save(any(Location.class))).thenThrow(new DataAccessException("Location save error") {});

        // Act & Assert: Expect an ApplicationException.
        ApplicationException ex = assertThrows(ApplicationException.class, () ->
                eventService.createEvent(creationDTO));
        assertNotNull(ex.getCause());
        assertTrue(ex.getMessage().contains("Failed to create event"));
    }

    /**
     * Test 3: Invites from friend tags and explicit invites are merged correctly,
     * ensuring that duplicate invites are not created.
     */
    @Test
    void createEvent_MergesInvites_Correctly() {
        // Arrange
        UUID creatorId = UUID.randomUUID();
        UUID friendTagId = UUID.randomUUID();
        // Both friend tag and explicit invite refer to the same user.
        UUID commonUserId = UUID.randomUUID();

        EventCreationDTO creationDTO = new EventCreationDTO(
                null,
                "Merged Invites Event",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                new LocationDTO(null, "Test Location", 0.0, 0.0),
                "Merged invites test",
                creatorId,
                List.of(friendTagId),
                List.of(commonUserId)
        );

        // Stub location creation using locationService.save.
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        when(locationService.save(any(Location.class))).thenReturn(location);

        // Stub creator lookup.
        User creator = new User();
        creator.setId(creatorId);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        // Stub eventRepository.save to simulate saving the event and generating a new event ID.
        Event savedEvent = new Event();
        UUID eventId = UUID.randomUUID();
        savedEvent.setId(eventId);
        savedEvent.setTitle("Merged Invites Event");
        savedEvent.setStartTime(creationDTO.startTime());
        savedEvent.setEndTime(creationDTO.endTime());
        savedEvent.setLocation(location);
        savedEvent.setNote("Merged invites test");
        savedEvent.setCreator(creator);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Stub friend tag lookup: when userService.getFriendUserIdsByFriendTagId is called with friendTagId,
        // return a list containing commonUserId.
        when(userService.getFriendUserIdsByFriendTagId(friendTagId)).thenReturn(List.of(commonUserId));

        // Stub chatMessageService: return an empty list for the new event.
        when(chatMessageService.getChatMessageIdsByEventId(eventId)).thenReturn(List.of());

        // Stub userRepository: when looking up the common invited user, return a User.
        User commonUser = new User();
        commonUser.setId(commonUserId);
        when(userRepository.findById(commonUserId)).thenReturn(Optional.of(commonUser));

        // Stub userService for final invited user IDs.
        when(userService.getInvitedUserIdsByEventId(eventId)).thenReturn(List.of(commonUserId));
        when(userService.getParticipantUserIdsByEventId(eventId)).thenReturn(List.of());

        // Act
        EventDTO eventDTO = (EventDTO) eventService.createEvent(creationDTO);

        // Assert
        assertNotNull(eventDTO);
        assertEquals("Merged Invites Event", eventDTO.title());
        // Verify that the final DTO's invited user IDs contain only the common user.
        assertEquals(1, eventDTO.invitedUserIds().size());
        assertTrue(eventDTO.invitedUserIds().contains(commonUserId));

        // Verify that eventUserRepository.save is called exactly once.
        verify(eventUserRepository, times(1)).save(any(EventUser.class));
    }
}
