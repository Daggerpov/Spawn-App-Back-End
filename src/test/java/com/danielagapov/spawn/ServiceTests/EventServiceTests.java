package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.EventCreationDTO;
import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
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
import com.danielagapov.spawn.Services.FriendTag.FriendTagService;
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
    private FriendTagService friendTagService;

    @Mock
    private IChatMessageService chatMessageService;

    @InjectMocks
    private EventService eventService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // --- Helper methods ---
    private Event createDummyEvent(UUID eventId, String title, OffsetDateTime start, OffsetDateTime end) {
        Location loc = new Location(UUID.randomUUID(), "Dummy Location", 0.0, 0.0);
        User creator = new User();
        creator.setId(UUID.randomUUID());
        return new Event(eventId, title, start, end, loc, "Note", creator);
    }

    private EventDTO dummyEventDTO(UUID eventId, String title) {
        return new EventDTO(
                eventId,
                title,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                UUID.randomUUID(),
                "Note",
                UUID.randomUUID(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private FullFeedEventDTO dummyFullFeedEventDTO(UUID eventId, String title) {
        return new FullFeedEventDTO(
                eventId,
                title,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                new LocationDTO(UUID.randomUUID(), "Location", 0.0, 0.0),
                "Note",
                new FullUserDTO(UUID.randomUUID(), List.of(), "username", "avatar.jpg", "first", "last", "bio", List.of(), "email"),
                List.of(),
                List.of(),
                List.of(),
                null,
                null
        );
    }

    // --- Basic tests (unchanged) ---
    @Test
    void getAllEvents_ShouldReturnList_WhenEventsExist() {
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        User dummyCreator = new User();
        dummyCreator.setId(UUID.randomUUID());
        Event event = new Event(UUID.randomUUID(), "Test Event",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                location, "Test note", dummyCreator);

        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(userService.getParticipantUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByEventId(any(UUID.class))).thenReturn(List.of());

        List<EventDTO> result = eventService.getAllEvents();

        assertFalse(result.isEmpty());
        // For EventDTO (record), use title() accessor.
        assertEquals("Test Event", result.get(0).title());
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void getEventById_ShouldReturnEvent_WhenEventExists() {
        UUID eventId = UUID.randomUUID();
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        User dummyCreator = new User();
        dummyCreator.setId(UUID.randomUUID());
        Event event = new Event(eventId, "Test Event",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                location, "Test note", dummyCreator);

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

        assertTrue(exception.getMessage().contains(eventId.toString()));
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
    void deleteEventById_ShouldReturnFalse_WhenDatabaseErrorOccurs() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.existsById(eventId)).thenReturn(true);
        doThrow(new DataAccessException("Database error") {}).when(eventRepository).deleteById(eventId);

        boolean result = eventService.deleteEventById(eventId);

        assertFalse(result);
        verify(eventRepository, times(1)).deleteById(eventId);
    }

    @Test
    void createEvent_Successful() {
        UUID creatorId = UUID.randomUUID();
        UUID friendTagId = UUID.randomUUID();
        UUID explicitInviteId = UUID.randomUUID();
        UUID friendTagUserId = UUID.randomUUID();

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

        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        when(locationService.save(any(Location.class))).thenReturn(location);

        User creator = new User();
        creator.setId(creatorId);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

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

        when(userService.getFriendUserIdsByFriendTagId(friendTagId)).thenReturn(List.of(friendTagUserId));
        when(chatMessageService.getChatMessageIdsByEventId(eventId)).thenReturn(List.of());

        User friendTagUser = new User();
        friendTagUser.setId(friendTagUserId);
        when(userRepository.findById(friendTagUserId)).thenReturn(Optional.of(friendTagUser));

        User explicitInvitedUser = new User();
        explicitInvitedUser.setId(explicitInviteId);
        when(userRepository.findById(explicitInviteId)).thenReturn(Optional.of(explicitInvitedUser));

        Set<UUID> expectedInvited = new HashSet<>(Arrays.asList(friendTagUserId, explicitInviteId));
        when(userService.getInvitedUserIdsByEventId(eventId)).thenReturn(new ArrayList<>(expectedInvited));

        EventDTO eventDTO = (EventDTO) eventService.createEvent(creationDTO);

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

    @Test
    void createEvent_Fails_WhenLocationNotCreated() {
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

        when(locationService.save(any(Location.class))).thenThrow(new DataAccessException("Location save error") {});

        ApplicationException ex = assertThrows(ApplicationException.class, () ->
                eventService.createEvent(creationDTO));
        assertNotNull(ex.getCause());
        assertTrue(ex.getMessage().contains("Failed to create event"));
    }

    @Test
    void createEvent_MergesInvites_Correctly() {
        UUID creatorId = UUID.randomUUID();
        UUID friendTagId = UUID.randomUUID();
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

        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        when(locationService.save(any(Location.class))).thenReturn(location);

        User creator = new User();
        creator.setId(creatorId);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

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

        when(userService.getFriendUserIdsByFriendTagId(friendTagId)).thenReturn(List.of(commonUserId));
        when(chatMessageService.getChatMessageIdsByEventId(eventId)).thenReturn(List.of());

        User commonUser = new User();
        commonUser.setId(commonUserId);
        when(userRepository.findById(commonUserId)).thenReturn(Optional.of(commonUser));

        when(userService.getInvitedUserIdsByEventId(eventId)).thenReturn(List.of(commonUserId));
        when(userService.getParticipantUserIdsByEventId(eventId)).thenReturn(List.of());

        EventDTO eventDTO = (EventDTO) eventService.createEvent(creationDTO);

        assertNotNull(eventDTO);
        assertEquals("Merged Invites Event", eventDTO.title());
        assertEquals(1, eventDTO.invitedUserIds().size());
        assertTrue(eventDTO.invitedUserIds().contains(commonUserId));

        verify(eventUserRepository, times(1)).save(any(EventUser.class));
    }

//    @Test
//    void getAllFullEvents_ShouldReturnFullFeedEvents_WhenEventsExist() {
//        Event event = createDummyEvent(UUID.randomUUID(), "Full Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
//        when(eventRepository.findAll()).thenReturn(List.of(event));
//        when(userService.getParticipantUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
//        when(userService.getInvitedUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
//        when(chatMessageService.getChatMessageIdsByEventId(any(UUID.class))).thenReturn(List.of());
//        when(locationService.getLocationById(any(UUID.class)))
//                .thenReturn(new LocationDTO(UUID.randomUUID(), "Location", 0.0, 0.0));
//        when(userService.getFullUserById(any(UUID.class))).thenReturn(new FullUserDTO(
//                UUID.randomUUID(), List.of(), "fullUsername", "avatar.jpg", "first", "last", "bio", List.of(), "email@example.com"));
//        when(userService.convertUsersToFullUsers(any(), eq(new HashSet<>()))).thenReturn(List.of());
//        when(chatMessageService.getFullChatMessagesByEventId(any(UUID.class))).thenReturn(List.of());
//        // Stub friend tag lookup; for events without a requesting user, no friend tag is applied.
//        when(friendTagService.getPertainingFriendTagByUserIds(any(UUID.class), any(UUID.class))).thenReturn(null);
//
//        // To ensure getParticipationStatus does not throw, stub existsById and findByEvent_Id.
//        when(eventUserRepository.existsById(any(UUID.class))).thenReturn(true);
//        // Return a list containing an EventUser with a dummy user (not matching any requesting user)
//        EventUser dummyEU = new EventUser();
//        User dummyUser = new User();
//        dummyUser.setId(UUID.randomUUID());
//        dummyEU.setUser(dummyUser);
//        dummyEU.setStatus(ParticipationStatus.invited);
//        when(eventUserRepository.findByEvent_Id(any(UUID.class))).thenReturn(List.of(dummyEU));
//
//        List<FullFeedEventDTO> fullEvents = eventService.getAllFullEvents();
//
//        assertNotNull(fullEvents);
//        assertFalse(fullEvents.isEmpty());
//        FullFeedEventDTO first = fullEvents.get(0);
//        assertEquals("Full Event", first.getTitle());
//        assertNull(first.getEventFriendTagColorHexCodeForRequestingUser());
//        assertNull(first.getParticipationStatus());
//    }

    @Test
    void getFullEventById_ShouldReturnFullFeedEventDTO_WhenEventExists() {
        UUID eventId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        Event event = createDummyEvent(eventId, "Detailed Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(userService.getParticipantUserIdsByEventId(eventId)).thenReturn(List.of());
        when(userService.getInvitedUserIdsByEventId(eventId)).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByEventId(eventId)).thenReturn(List.of());
        when(locationService.getLocationById(any(UUID.class)))
                .thenReturn(new LocationDTO(UUID.randomUUID(), "Location", 0.0, 0.0));
        FullUserDTO fullUser = new FullUserDTO(
                UUID.randomUUID(), List.of(), "fullUsername", "avatar.jpg", "first", "last", "bio", List.of(), "email@example.com");
        when(userService.getFullUserById(any(UUID.class))).thenReturn(fullUser);
        when(userService.convertUsersToFullUsers(any(), eq(new HashSet<>()))).thenReturn(List.of());
        when(chatMessageService.getFullChatMessagesByEventId(eventId)).thenReturn(List.of());
        // Stub friend tag lookup
        com.danielagapov.spawn.DTOs.FriendTagDTO friendTag = mock(com.danielagapov.spawn.DTOs.FriendTagDTO.class);
        when(friendTag.colorHexCode()).thenReturn("#123456");
        when(friendTagService.getPertainingFriendTagByUserIds(requestingUserId, event.getCreator().getId()))
                .thenReturn(friendTag);
        // Stub eventUserRepository.existsById and findByEvent_Id so that participation status is calculated.
        when(eventUserRepository.existsById(eventId)).thenReturn(true);
        EventUser eu = new EventUser();
        User euUser = new User();
        euUser.setId(requestingUserId);
        eu.setUser(euUser);
        eu.setStatus(ParticipationStatus.participating);
        when(eventUserRepository.findByEvent_Id(eventId)).thenReturn(List.of(eu));

        FullFeedEventDTO fullEvent = eventService.getFullEventById(eventId, requestingUserId);

        assertNotNull(fullEvent);
        assertEquals("Detailed Event", fullEvent.getTitle());
//        assertEquals("#123456", fullEvent.getEventFriendTagColorHexCodeForRequestingUser());
//        assertEquals(ParticipationStatus.participating, fullEvent.getParticipationStatus());
    }

    @Test
    void getEventsByFriendTagId_ShouldReturnEvents_WhenFriendsExist() {
        UUID tagId = UUID.randomUUID();
        com.danielagapov.spawn.DTOs.FriendTagDTO friendTag = mock(com.danielagapov.spawn.DTOs.FriendTagDTO.class);
        List<UUID> friendIds = List.of(UUID.randomUUID());
        when(friendTag.friendUserIds()).thenReturn(friendIds);
        when(friendTagService.getFriendTagById(tagId)).thenReturn(friendTag);

        Event event = createDummyEvent(UUID.randomUUID(), "Friend Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        when(eventRepository.findByCreatorIdIn(friendIds)).thenReturn(List.of(event));
        when(userService.getParticipantUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByEventId(any(UUID.class))).thenReturn(List.of());

        List<EventDTO> events = eventService.getEventsByFriendTagId(tagId);

        assertNotNull(events);
        assertFalse(events.isEmpty());
        assertEquals("Friend Event", events.get(0).title());
    }

    @Test
    void getEventsByOwnerId_ShouldReturnEvents_WhenOwnerExists() {
        UUID ownerId = UUID.randomUUID();
        Event event = createDummyEvent(UUID.randomUUID(), "Owner Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        when(eventRepository.findByCreatorId(ownerId)).thenReturn(List.of(event));
        when(userService.getParticipantUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByEventId(any(UUID.class))).thenReturn(List.of());

        List<EventDTO> events = eventService.getEventsByOwnerId(ownerId);

        assertNotNull(events);
        assertFalse(events.isEmpty());
        assertEquals("Owner Event", events.get(0).title());
    }

    @Test
    void replaceEvent_ShouldReplaceEvent_WhenEventExists() {
        UUID eventId = UUID.randomUUID();
        Event existingEvent = createDummyEvent(eventId, "Old Title", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

        EventDTO newEventDTO = dummyEventDTO(eventId, "New Title");
        Location dummyLoc = new Location(UUID.randomUUID(), "New Location", 10.0, 20.0);
        when(locationService.getLocationEntityById(newEventDTO.locationId())).thenReturn(dummyLoc);
        User dummyCreator = new User();
        dummyCreator.setId(newEventDTO.creatorUserId());
        when(userService.getUserEntityById(newEventDTO.creatorUserId())).thenReturn(dummyCreator);

        Event updatedEvent = createDummyEvent(eventId, "New Title", newEventDTO.startTime(), newEventDTO.endTime());
        updatedEvent.setLocation(dummyLoc);
        updatedEvent.setCreator(dummyCreator);
        when(eventRepository.save(existingEvent)).thenReturn(updatedEvent);

        EventDTO result = eventService.replaceEvent(newEventDTO, eventId);

        assertNotNull(result);
        assertEquals("New Title", result.title());
    }

    @Test
    void replaceEvent_ShouldCreateNewEvent_WhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        EventDTO newEventDTO = dummyEventDTO(eventId, "Created Event");
        Location dummyLoc = new Location(UUID.randomUUID(), "Location", 0.0, 0.0);
        when(locationService.getLocationEntityById(newEventDTO.locationId())).thenReturn(dummyLoc);
        User dummyCreator = new User();
        dummyCreator.setId(newEventDTO.creatorUserId());
        when(userService.getUserEntityById(newEventDTO.creatorUserId())).thenReturn(dummyCreator);

        Event newEvent = createDummyEvent(eventId, "Created Event", newEventDTO.startTime(), newEventDTO.endTime());
        newEvent.setLocation(dummyLoc);
        newEvent.setCreator(dummyCreator);
        when(eventRepository.save(any(Event.class))).thenReturn(newEvent);

        EventDTO result = eventService.replaceEvent(newEventDTO, eventId);

        assertNotNull(result);
        assertEquals("Created Event", result.title());
    }

    @Test
    void getParticipatingUsersByEventId_ShouldReturnUserDTOs_WhenParticipantsExist() {
        UUID eventId = UUID.randomUUID();
        EventUser eu1 = new EventUser();
        User user1 = new User();
        user1.setId(UUID.randomUUID());
        eu1.setUser(user1);
        eu1.setStatus(ParticipationStatus.participating);
        EventUser eu2 = new EventUser();
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        eu2.setUser(user2);
        eu2.setStatus(ParticipationStatus.invited);

        when(eventUserRepository.findByEvent_Id(eventId)).thenReturn(List.of(eu1, eu2));
        com.danielagapov.spawn.DTOs.UserDTO userDTO1 = new com.danielagapov.spawn.DTOs.UserDTO(
                user1.getId(), List.of(), "user1", "pic.jpg", "First", "Last", "bio", List.of(), "email1@example.com");
        when(userService.getUserById(user1.getId())).thenReturn(userDTO1);

        List<com.danielagapov.spawn.DTOs.UserDTO> participants = eventService.getParticipatingUsersByEventId(eventId);

        assertNotNull(participants);
        assertEquals(1, participants.size());
        assertEquals(user1.getId(), participants.get(0).getId());
    }

    @Test
    void getParticipationStatus_ShouldReturnStatus_WhenUserParticipates() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // Stub existsById to return true so that the method proceeds.
        when(eventUserRepository.existsById(eventId)).thenReturn(true);
        EventUser eu = new EventUser();
        User user = new User();
        user.setId(userId);
        eu.setUser(user);
        eu.setStatus(ParticipationStatus.participating);
        when(eventUserRepository.findByEvent_Id(eventId)).thenReturn(List.of(eu));

        ParticipationStatus status = eventService.getParticipationStatus(eventId, userId);

        assertEquals(ParticipationStatus.participating, status);
    }

    @Test
    void getParticipationStatus_ShouldReturnNotInvited_WhenUserNotFound() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // Stub existsById to return true and provide a list with a user not matching userId.
        when(eventUserRepository.existsById(eventId)).thenReturn(true);
        EventUser eu = new EventUser();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        eu.setUser(otherUser);
        eu.setStatus(ParticipationStatus.invited);
        when(eventUserRepository.findByEvent_Id(eventId)).thenReturn(List.of(eu));

        ParticipationStatus status = eventService.getParticipationStatus(eventId, userId);

        assertEquals(ParticipationStatus.notInvited, status);
    }

    @Test
    void inviteUser_ShouldInviteUser_WhenNotAlreadyInvited() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // Return a non-empty list with an EventUser for a different user.
        EventUser otherEU = new EventUser();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherEU.setUser(otherUser);
        otherEU.setStatus(ParticipationStatus.participating);
        when(eventUserRepository.findByEvent_Id(eventId)).thenReturn(List.of(otherEU));

        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Event event = createDummyEvent(eventId, "Invite Test", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        boolean alreadyInvited = eventService.inviteUser(eventId, userId);

        assertFalse(alreadyInvited);
        verify(eventUserRepository, times(1)).save(any(EventUser.class));
    }

    @Test
    void inviteUser_ShouldReturnTrue_WhenUserAlreadyInvited() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EventUser eu = new EventUser();
        User user = new User();
        user.setId(userId);
        eu.setUser(user);
        eu.setStatus(ParticipationStatus.invited);
        when(eventUserRepository.findByEvent_Id(eventId)).thenReturn(List.of(eu));

        boolean result = eventService.inviteUser(eventId, userId);

        assertTrue(result);
        verify(eventUserRepository, never()).save(any(EventUser.class));
    }

    @Test
    void getEventsInvitedTo_ShouldReturnEvents_WhenUserIsInvited() {
        UUID userId = UUID.randomUUID();
        Event event = createDummyEvent(UUID.randomUUID(), "Invited Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        EventUser eu = new EventUser();
        User user = new User();
        user.setId(userId);
        eu.setUser(user);
        eu.setEvent(event);
        when(eventUserRepository.findByUser_Id(userId)).thenReturn(List.of(eu));
        when(userService.getParticipantUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByEventId(any(UUID.class))).thenReturn(List.of());

        List<EventDTO> events = eventService.getEventsInvitedTo(userId);

        assertNotNull(events);
        assertEquals(1, events.size());
    }

    @Test
    void getFullEventsInvitedTo_ShouldReturnFullEvents_WhenUserIsInvited() {
        UUID userId = UUID.randomUUID();
        Event event = createDummyEvent(UUID.randomUUID(), "Full Invited Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));

        // Stub participation with valid EventUser
        when(eventUserRepository.existsById(any(UUID.class))).thenReturn(true);
        EventUser validEventUser = new EventUser();
        User invitedUser = new User();
        invitedUser.setId(userId);
        validEventUser.setUser(invitedUser);
        validEventUser.setStatus(ParticipationStatus.invited);
        validEventUser.setEvent(event);
        when(eventUserRepository.findByEvent_Id(any(UUID.class))).thenReturn(List.of(validEventUser));
        when(eventUserRepository.findByUser_Id(userId)).thenReturn(List.of(validEventUser));

        when(userService.getParticipantUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedUserIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByEventId(any(UUID.class))).thenReturn(List.of());
        when(locationService.getLocationById(any(UUID.class)))
                .thenReturn(new LocationDTO(UUID.randomUUID(), "Location", 0.0, 0.0));
        when(userService.getFullUserById(any(UUID.class)))
                .thenReturn(new FullUserDTO(UUID.randomUUID(), List.of(), "fullUsername", "avatar.jpg", "first", "last", "bio", List.of(), "email@example.com"));
        when(userService.convertUsersToFullUsers(any(), eq(new HashSet<>()))).thenReturn(List.of());
        when(chatMessageService.getFullChatMessagesByEventId(any(UUID.class))).thenReturn(List.of());

        com.danielagapov.spawn.DTOs.FriendTagDTO dummyTag = mock(com.danielagapov.spawn.DTOs.FriendTagDTO.class);
        when(dummyTag.colorHexCode()).thenReturn("#DUMMY");
        when(friendTagService.getPertainingFriendTagByUserIds(any(UUID.class), any(UUID.class))).thenReturn(dummyTag);

        List<FullFeedEventDTO> fullEvents = eventService.getFullEventsInvitedTo(userId);

        assertNotNull(fullEvents);
        assertFalse(fullEvents.isEmpty());
    }

//    @Test
//    void getFullEventByEvent_ShouldReturnFullFeedEventDTO() {
//        UUID eventId = UUID.randomUUID();
//        EventDTO eventDTO = new EventDTO(
//                eventId,
//                "Some Event",
//                OffsetDateTime.now(),
//                OffsetDateTime.now().plusHours(1),
//                UUID.randomUUID(),
//                "Note",
//                UUID.randomUUID(),
//                List.of(), List.of(), List.of());
//        when(locationService.getLocationById(eventDTO.locationId()))
//                .thenReturn(new LocationDTO(UUID.randomUUID(), "Location", 0.0, 0.0));
//        FullUserDTO fullUser = new FullUserDTO(
//                eventDTO.creatorUserId(), List.of(), "fullUsername", "avatar.jpg", "first", "last", "bio", List.of(), "email@example.com");
//        when(userService.getFullUserById(eventDTO.creatorUserId())).thenReturn(fullUser);
//        when(userService.getParticipantsByEventId(eventDTO.id())).thenReturn(List.of());
//        when(userService.getInvitedByEventId(eventDTO.id())).thenReturn(List.of());
//        when(userService.convertUsersToFullUsers(any(), eq(new HashSet<>()))).thenReturn(List.of());
//        when(chatMessageService.getFullChatMessagesByEventId(eventDTO.id())).thenReturn(List.of());
//
//        FullFeedEventDTO fullEvent = eventService.getFullEventByEvent(eventDTO, null, new HashSet<>());
//
//        assertNotNull(fullEvent);
//        // For EventDTO record, use eventDTO.title() accessor.
//        assertEquals(eventDTO.title(), fullEvent.getTitle());
//        assertNull(fullEvent.getEventFriendTagColorHexCodeForRequestingUser());
//        assertNull(fullEvent.getParticipationStatus());
//    }

    @Test
    void getFriendTagColorHexCodeForRequestingUser_ShouldReturnColorHexCode() {
        UUID creatorId = UUID.randomUUID();
        EventDTO eventDTO = new EventDTO(
                UUID.randomUUID(), "Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                UUID.randomUUID(), "Note", creatorId, List.of(), List.of(), List.of());
        UUID requestingUserId = UUID.randomUUID();
        com.danielagapov.spawn.DTOs.FriendTagDTO friendTag = mock(com.danielagapov.spawn.DTOs.FriendTagDTO.class);
        when(friendTag.colorHexCode()).thenReturn("#ABCDEF");
        when(friendTagService.getPertainingFriendTagByUserIds(requestingUserId, creatorId)).thenReturn(friendTag);

        String colorHex = eventService.getFriendTagColorHexCodeForRequestingUser(eventDTO, requestingUserId);

        assertEquals("#ABCDEF", colorHex);
    }

    @Test
    void convertEventsToFullFeedEvents_ShouldReturnConvertedList() {
        EventDTO eventDTO1 = dummyEventDTO(UUID.randomUUID(), "Event 1");
        EventDTO eventDTO2 = dummyEventDTO(UUID.randomUUID(), "Event 2");
        List<EventDTO> events = List.of(eventDTO1, eventDTO2);
        UUID requestingUserId = UUID.randomUUID();

        when(locationService.getLocationById(any(UUID.class)))
                .thenReturn(new LocationDTO(UUID.randomUUID(), "Location", 0.0, 0.0));
        when(userService.getFullUserById(any(UUID.class)))
                .thenReturn(new FullUserDTO(UUID.randomUUID(), List.of(), "fullUsername", "avatar.jpg",
                        "first", "last", "bio", List.of(), "email@example.com"));
        when(userService.getParticipantsByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.convertUsersToFullUsers(any(), eq(new HashSet<>()))).thenReturn(List.of());
        when(chatMessageService.getFullChatMessagesByEventId(any(UUID.class))).thenReturn(List.of());
        // Stub participation: existsById true and findByEvent_Id returns a dummy EventUser not matching the requesting user.
        when(eventUserRepository.existsById(any(UUID.class))).thenReturn(true);
        EventUser dummyEU = new EventUser();
        User dummyUser = new User();
        dummyUser.setId(UUID.randomUUID()); // not equal to requestingUserId
        dummyEU.setUser(dummyUser);
        dummyEU.setStatus(ParticipationStatus.invited);
        when(eventUserRepository.findByEvent_Id(any(UUID.class))).thenReturn(List.of(dummyEU));
        // Stub friend tag lookup to return null (i.e. no friend tag applies).
        when(friendTagService.getPertainingFriendTagByUserIds(any(UUID.class), any(UUID.class))).thenReturn(null);

        List<FullFeedEventDTO> fullEvents = eventService.convertEventsToFullFeedEvents(events, requestingUserId);
        assertNotNull(fullEvents, "The converted list should not be null");
        assertEquals(2, fullEvents.size(), "There should be two full events in the converted list");
    }

    @Test
    void convertEventsToFullFeedSelfOwnedEvents_ShouldReturnConvertedListWithAccent() {
        EventDTO eventDTO1 = dummyEventDTO(UUID.randomUUID(), "Self-Owned Event");
        List<EventDTO> events = List.of(eventDTO1);
        UUID requestingUserId = UUID.randomUUID();

        when(locationService.getLocationById(any(UUID.class)))
                .thenReturn(new LocationDTO(UUID.randomUUID(), "Location", 0.0, 0.0));
        when(userService.getFullUserById(any(UUID.class)))
                .thenReturn(new FullUserDTO(UUID.randomUUID(), List.of(), "fullUsername", "avatar.jpg", "first", "last", "bio", List.of(), "email@example.com"));
        when(userService.getParticipantsByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedByEventId(any(UUID.class))).thenReturn(List.of());
        when(userService.convertUsersToFullUsers(any(), eq(new HashSet<>()))).thenReturn(List.of());
        when(chatMessageService.getFullChatMessagesByEventId(any(UUID.class))).thenReturn(List.of());

        // Stub friend tag lookup to return null (self-owned accent)
        when(friendTagService.getPertainingFriendTagByUserIds(any(UUID.class), any(UUID.class))).thenReturn(null);

        // Stub participation lookup with a valid EventUser and User
        when(eventUserRepository.existsById(any(UUID.class))).thenReturn(true);
        EventUser validEventUser = new EventUser();
        User validUser = new User();
        validUser.setId(UUID.randomUUID());
        validEventUser.setUser(validUser);
        validEventUser.setStatus(ParticipationStatus.participating);
        when(eventUserRepository.findByEvent_Id(any(UUID.class))).thenReturn(List.of(validEventUser));

        List<FullFeedEventDTO> fullEvents = eventService.convertEventsToFullFeedSelfOwnedEvents(events, requestingUserId);

        assertNotNull(fullEvents);
        assertEquals(1, fullEvents.size());
        assertEquals("#1D3D3D", fullEvents.get(0).getEventFriendTagColorHexCodeForRequestingUser());
    }

    @Test
    void toggleParticipation_ShouldToggleStatus_WhenUserIsInvitedOrParticipating() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventUser invitedEventUser = new EventUser();
        User user = new User();
        user.setId(userId);
        invitedEventUser.setUser(user);
        invitedEventUser.setStatus(ParticipationStatus.invited);

        Event event = new Event();
        event.setId(eventId);

        User creator = new User();
        creator.setId(UUID.randomUUID());
        event.setCreator(creator);

        invitedEventUser.setEvent(event);

        when(eventUserRepository.existsById(eventId)).thenReturn(true); // Added mock to prevent BaseNotFoundException
        when(eventUserRepository.findByEvent_Id(eventId)).thenReturn(List.of(invitedEventUser));
        when(eventUserRepository.save(any(EventUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event)); // Mock event lookup

        FullFeedEventDTO result = eventService.toggleParticipation(eventId, userId);
        assertNotNull(result);
        assertEquals(ParticipationStatus.participating, invitedEventUser.getStatus());

        result = eventService.toggleParticipation(eventId, userId);
        assertNotNull(result);
        assertEquals(ParticipationStatus.invited, invitedEventUser.getStatus());
    }
}
