package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.*;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.CompositeKeys.EventUsersId;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.Location.ILocationService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class EventService implements IEventService {
    private final IEventRepository repository;
    private final ILocationRepository locationRepository;
    private final IEventUserRepository eventUserRepository;
    private final IUserRepository userRepository;
    private final IFriendTagService friendTagService;
    private final IUserService userService;
    private final IChatMessageService chatMessageService;
    private final ILogger logger;
    private final ILocationService locationService;

    @Autowired
    @Lazy // avoid circular dependency problems with ChatMessageService
    public EventService(IEventRepository repository, ILocationRepository locationRepository,
                        IEventUserRepository eventUserRepository, IUserRepository userRepository,
                        IFriendTagService friendTagService, IUserService userService, IChatMessageService chatMessageService,
                        ILogger logger, ILocationService locationService) {
        this.repository = repository;
        this.locationRepository = locationRepository;
        this.eventUserRepository = eventUserRepository;
        this.userRepository = userRepository;
        this.friendTagService = friendTagService;
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.logger = logger;
        this.locationService = locationService;
    }

    @Override
    public List<FullFeedEventDTO> getAllFullEvents() {
        System.out.println("Fetching all full events");
        ArrayList<FullFeedEventDTO> fullEvents = new ArrayList<>();
        for (EventDTO e : getAllEvents()) {
            fullEvents.add(getFullEventByEvent(e, null, new HashSet<>()));
        }
        System.out.println("Full events fetched: " + fullEvents);
        return fullEvents;
    }

    @Override
    public List<EventDTO> getAllEvents() {
        try {
            System.out.println("Fetching all events");
            List<Event> events = repository.findAll();
            System.out.println("Events fetched: " + events);
            return getEventDTOS(events);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            System.err.println("Data access error: " + e.getMessage());
            throw new BasesNotFoundException(EntityType.Event);
        } catch (Exception e) {
            logger.log(e.getMessage());
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public EventDTO getEventById(UUID id) {
        System.out.println("Fetching event by ID: " + id);
        Event event = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, id));
        System.out.println("Event found: " + event);

        UUID creatorUserId = event.getCreator().getId();
        List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(id);
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(id);
        List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(id);

        EventDTO eventDTO = EventMapper.toDTO(event, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
        System.out.println("Event DTO created: " + eventDTO);
        return eventDTO;
    }

    @Override
    public FullFeedEventDTO getFullEventById(UUID id, UUID requestingUserId) {
        System.out.println("Fetching full event by ID: " + id + " for user: " + requestingUserId);
        return getFullEventByEvent(getEventById(id), requestingUserId, new HashSet<>());
    }

    @Override
    public List<EventDTO> getEventsByFriendTagId(UUID tagId) {
        try {
            System.out.println("Fetching events by friend tag ID: " + tagId);
            // Step 1: Retrieve the FriendTagDTO and its associated friend user IDs
            FriendTagDTO friendTag = friendTagService.getFriendTagById(tagId);
            List<UUID> friendIds = friendTag.friendUserIds();
            System.out.println("Friend IDs: " + friendIds);

            // Step 2: Retrieve events created by any of the friends
            // Step 3: Filter events based on whether their owner is in the list of friend
            // IDs
            List<Event> filteredEvents = repository.findByCreatorIdIn(friendIds);
            System.out.println("Filtered events: " + filteredEvents);

            // Step 3: Map filtered events to detailed DTOs
            List<EventDTO> eventDTOs = filteredEvents.stream()
                    .map(event -> EventMapper.toDTO(
                            event,
                            event.getCreator().getId(),
                            userService.getParticipantUserIdsByEventId(event.getId()),
                            userService.getInvitedUserIdsByEventId(event.getId()),
                            chatMessageService.getChatMessageIdsByEventId(event.getId())))
                    .toList();
            System.out.println("Event DTOs created: " + eventDTOs);
            return eventDTOs;
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            System.err.println("Data access error: " + e.getMessage());
            throw new RuntimeException("Error retrieving events by friend tag ID", e);
        } catch (BaseNotFoundException e) {
            logger.log(e.getMessage());
            System.err.println("Base not found error: " + e.getMessage());
            throw e; // Rethrow if it's a custom not-found exception
        } catch (Exception e) {
            logger.log(e.getMessage());
            System.err.println("Unexpected error: " + e.getMessage());
            throw new RuntimeException("Unexpected error", e);
        }
    }

    @Override
    public IEventDTO saveEvent(IEventDTO event) {
        try {
            System.out.println("Saving event: " + event);
            Event eventEntity;

            if (event instanceof FullFeedEventDTO fullFeedEventDTO) {
                eventEntity = EventMapper.convertFullFeedEventDTOToEventEntity(fullFeedEventDTO);
            } else if (event instanceof EventDTO eventDTO) {
                Location location = locationRepository.findById(eventDTO.locationId()).orElse(null);

                // Map EventDTO to Event entity with the resolved Location
                eventEntity = EventMapper.toEntity(eventDTO, location,
                        userService.getUserEntityById(eventDTO.creatorUserId()));
            } else {
                throw new IllegalArgumentException("Unsupported event type");
            }

            // Save the Event entity
            eventEntity = repository.save(eventEntity);
            System.out.println("Event entity saved: " + eventEntity);

            // Map saved Event entity back to EventDTO with all necessary fields
            IEventDTO eventDTO = EventMapper.toDTO(
                    eventEntity,
                    eventEntity.getCreator().getId(), // creatorUserId
                    userService.getParticipantUserIdsByEventId(eventEntity.getId()), // participantUserIds
                    userService.getInvitedUserIdsByEventId(eventEntity.getId()), // invitedUserIds
                    chatMessageService.getChatMessageIdsByEventId(eventEntity.getId()) // chatMessageIds
            );
            System.out.println("Event DTO created: " + eventDTO);
            return eventDTO;
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            System.err.println("Data access error: " + e.getMessage());
            throw new BaseSaveException("Failed to save event: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public IEventDTO createEvent(EventCreationDTO eventCreationDTO) {
        try {
            System.out.println("Creating event with data: " + eventCreationDTO);

            Location location = locationService.save(LocationMapper.toEntity(eventCreationDTO.location()));
            System.out.println("Location saved: " + location);

            User creator = userRepository.findById(eventCreationDTO.creatorUserId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, eventCreationDTO.creatorUserId()));
            System.out.println("Creator found: " + creator);

            Event event = EventMapper.fromCreationDTO(eventCreationDTO, location, creator);
            System.out.println("Event mapped from DTO: " + event);

            event = repository.save(event);
            System.out.println("Event saved: " + event);

            Set<UUID> allInvitedUserIds = new HashSet<>();
            if (eventCreationDTO.invitedFriendTagIds() != null) {
                for (UUID friendTagId : eventCreationDTO.invitedFriendTagIds()) {
                    List<UUID> friendIdsForTag = userService.getFriendUserIdsByFriendTagId(friendTagId);
                    allInvitedUserIds.addAll(friendIdsForTag);
                    System.out.println("Friend IDs for tag " + friendTagId + ": " + friendIdsForTag);
                }
            }
            if (eventCreationDTO.invitedFriendUserIds() != null) {
                allInvitedUserIds.addAll(eventCreationDTO.invitedFriendUserIds());
                System.out.println("Invited friend user IDs: " + eventCreationDTO.invitedFriendUserIds());
            }

            for (UUID userId : allInvitedUserIds) {
                User invitedUser = userRepository.findById(userId)
                        .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
                EventUsersId compositeId = new EventUsersId(event.getId(), userId);
                EventUser eventUser = new EventUser();
                eventUser.setId(compositeId);
                eventUser.setEvent(event);
                eventUser.setUser(invitedUser);
                eventUser.setStatus(ParticipationStatus.invited);
                eventUserRepository.save(eventUser);
                System.out.println("Event user saved: " + eventUser);
            }

            IEventDTO eventDTO = EventMapper.toDTO(event, creator.getId(), null, new ArrayList<>(allInvitedUserIds), null);
            System.out.println("Event DTO created: " + eventDTO);
            return eventDTO;
        } catch (Exception e) {
            logger.log("Error creating event: " + e.getMessage());
            System.err.println("Error creating event: " + e.getMessage());
            throw new ApplicationException("Failed to create event", e);
        }
    }

    @Override
    public List<EventDTO> getEventsByOwnerId(UUID creatorUserId) {
        System.out.println("Fetching events by owner ID: " + creatorUserId);
        List<Event> events = repository.findByCreatorId(creatorUserId);
        System.out.println("Events found: " + events);

        List<EventDTO> eventDTOs = getEventDTOS(events);
        System.out.println("Event DTOs created: " + eventDTOs);
        return eventDTOs;
    }

    private List<EventDTO> getEventDTOS(List<Event> events) {
        System.out.println("Converting events to DTOs: " + events);
        List<EventDTO> eventDTOs = new ArrayList<>();

        for (Event event : events) {
            UUID eventId = event.getId();

            // Fetch related data for the current event
            UUID creatorUserId = event.getCreator().getId();
            List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(eventId);
            List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(eventId);
            List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(eventId);

            // Map the event to its DTO
            EventDTO eventDTO = EventMapper.toDTO(event, creatorUserId, participantUserIds, invitedUserIds,
                    chatMessageIds);
            eventDTOs.add(eventDTO);
        }

        System.out.println("Event DTOs: " + eventDTOs);
        return eventDTOs;
    }

    @Override
    public EventDTO replaceEvent(EventDTO newEvent, UUID id) {
        System.out.println("Replacing event with ID: " + id + " with new event: " + newEvent);
        return repository.findById(id).map(event -> {
            // Update basic event details
            event.setTitle(newEvent.title());
            event.setNote(newEvent.note());
            event.setEndTime(newEvent.endTime());
            event.setStartTime(newEvent.startTime());

            // Fetch the location entity by locationId from DTO
            event.setLocation(locationService.getLocationEntityById(newEvent.locationId()));

            // Save updated event
            repository.save(event);
            System.out.println("Event updated: " + event);

            return constructDTOFromEntity(event);
        }).orElseGet(() -> {
            // Map and save new event, fetch location and creator
            Location location = locationService.getLocationEntityById(newEvent.locationId());
            User creator = userService.getUserEntityById(newEvent.creatorUserId());

            // Convert DTO to entity
            Event eventEntity = EventMapper.toEntity(newEvent, location, creator);
            repository.save(eventEntity);
            System.out.println("New event saved: " + eventEntity);

            return constructDTOFromEntity(eventEntity);
        });
    }

    private EventDTO constructDTOFromEntity(Event eventEntity) {
        // Fetch related data for DTO
        UUID creatorUserId = eventEntity.getCreator().getId();
        List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(eventEntity.getId());
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(eventEntity.getId());
        List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(eventEntity.getId());

        EventDTO eventDTO = EventMapper.toDTO(eventEntity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
        System.out.println("Constructed EventDTO from entity: " + eventDTO);
        return eventDTO;
    }

    @Override
    public boolean deleteEventById(UUID id) {
        System.out.println("Deleting event by ID: " + id);
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.Event, id);
        }

        try {
            repository.deleteById(id);
            System.out.println("Event deleted successfully");
            return true;
        } catch (Exception e) {
            logger.log(e.getMessage());
            System.err.println("Error deleting event: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<UserDTO> getParticipatingUsersByEventId(UUID eventId) {
        System.out.println("Fetching participating users by event ID: " + eventId);
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);

            if (eventUsers.isEmpty()) {
                throw new BaseNotFoundException(EntityType.Event, eventId);
            }

            List<UserDTO> userDTOs = eventUsers.stream()
                    .filter(eventUser -> eventUser.getStatus().equals(ParticipationStatus.participating))
                    .map(eventUser -> userService.getUserById(eventUser.getUser().getId()))
                    .toList();
            System.out.println("Participating users: " + userDTOs);
            return userDTOs;
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            System.err.println("Data access error: " + e.getMessage());
            throw new BaseNotFoundException(EntityType.Event, eventId);
        } catch (Exception e) {
            logger.log(e.getMessage());
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ParticipationStatus getParticipationStatus(UUID eventId, UUID userId) {
        System.out.println("Fetching participation status for event ID: " + eventId + " and user ID: " + userId);
        if (!eventUserRepository.existsById(eventId)) {
            throw new BaseNotFoundException(EntityType.Event, eventId);
        }

        List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);
        if (eventUsers.isEmpty()) {
            throw new BaseNotFoundException(EntityType.Event, eventId);
        }

        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(userId)) {
                System.out.println("Participation status found: " + eventUser.getStatus());
                return eventUser.getStatus();
            }
        }

        System.out.println("User not invited");
        return ParticipationStatus.notInvited;
    }

    // return type boolean represents whether the user was already invited or not
    // if false -> invites them
    // if true -> return 400 in Controller to indicate that the user has already
    // been invited, or it is a bad request.
    @Override
    public boolean inviteUser(UUID eventId, UUID userId) {
        System.out.println("Inviting user ID: " + userId + " to event ID: " + eventId);
        List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);
        if (eventUsers.isEmpty()) {
            // throw BaseNotFound for events if eventId has no EventUsers
            throw new BaseNotFoundException(EntityType.Event, eventId);
        }

        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(userId)) {
                System.out.println("User already invited: " + eventUser.getStatus().equals(ParticipationStatus.invited));
                // user is already in list
                return eventUser.getStatus().equals(ParticipationStatus.invited);
            } else {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, eventId));
                Event event = repository.findById(eventId)
                        .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));

                EventUser newEventUser = new EventUser();
                eventUser.setEvent(event);
                eventUser.setUser(user);
                eventUser.setStatus(ParticipationStatus.invited);

                eventUserRepository.save(newEventUser);
                System.out.println("User invited successfully");
                return false;
            }
        }
        System.out.println("Bad request: user already invited");
        // if the loop doesn't return, it's a bad request
        return true;
    }

    // returns the updated event, with modified participants and invited users
    // invited/participating
    // if true -> change status
    // if false -> return 400 in controller to indicate that the user is not
    // invited/participating
    @Override
    public FullFeedEventDTO toggleParticipation(UUID eventId, UUID userId) {
        System.out.println("Toggling participation for event ID: " + eventId + " and user ID: " + userId);
        List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);
        if (eventUsers.isEmpty()) {
            // throw BaseNotFound for events if eventIf has no eventUsers
            throw new BaseNotFoundException(EntityType.Event, eventId);
        }
        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(userId) && !eventUser.getStatus().equals(ParticipationStatus.notInvited)) {
                // if invited -> set status to participating
                // if participating -> set status to invited
                if (eventUser.getStatus().equals(ParticipationStatus.invited)) {
                    eventUser.setStatus(ParticipationStatus.participating);
                } else if (eventUser.getStatus().equals(ParticipationStatus.participating)) {
                    eventUser.setStatus(ParticipationStatus.invited);
                }
                eventUserRepository.save(eventUser);
                System.out.println("Participation status toggled: " + eventUser.getStatus());
                break;
            }
        }
        return getFullEventById(eventId, userId);
    }

    @Override
    public List<EventDTO> getEventsInvitedTo(UUID id) {
        System.out.println("Fetching events invited to for user ID: " + id);
        List<EventUser> eventUsers = eventUserRepository.findByUser_Id(id);

        List<Event> events = new ArrayList<>();

        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(id)) {
                events.add(eventUser.getEvent());
            }
        }

        List<EventDTO> eventDTOs = getEventDTOS(events);
        System.out.println("Events invited to: " + eventDTOs);
        return eventDTOs;
    }

    @Override
    public List<FullFeedEventDTO> getFullEventsInvitedTo(UUID id) {
        System.out.println("Fetching full events invited to for user ID: " + id);
        List<EventUser> eventUsers = eventUserRepository.findByUser_Id(id);

        List<Event> events = new ArrayList<>();

        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(id) && eventUser.getStatus() != ParticipationStatus.notInvited) {
                events.add(eventUser.getEvent());
            }
        }

        List<EventDTO> eventDTOs = getEventDTOS(events);

        List<FullFeedEventDTO> fullFeedEventDTOs = eventDTOs.stream()
                .map(eventDTO -> getFullEventByEvent(eventDTO, id, new HashSet<>()))
                .toList();
        System.out.println("Full events invited to: " + fullFeedEventDTOs);
        return fullFeedEventDTOs;
    }

    /**
     * @param requestingUserId
     * @return This method returns the feed events for a user, with their created ones
     * first in the `universalAccentColor`, followed by events they're invited to
     */
    @Override
    public List<FullFeedEventDTO> getFeedEvents(UUID requestingUserId) {
        try {
            System.out.println("Fetching feed events for user ID: " + requestingUserId);
            List<FullFeedEventDTO> eventsCreated =
                    convertEventsToFullFeedSelfOwnedEvents(getEventsByOwnerId(requestingUserId), requestingUserId);
            List<FullFeedEventDTO> eventsInvitedTo = getFullEventsInvitedTo(requestingUserId);

            OffsetDateTime now = OffsetDateTime.now();

            eventsCreated.removeIf(event -> event.getEndTime() != null && event.getEndTime().isBefore(now));
            eventsInvitedTo.removeIf(event -> event.getEndTime() != null && event.getEndTime().isBefore(now));

            eventsCreated.sort(Comparator.comparing(FullFeedEventDTO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
            eventsInvitedTo.sort(Comparator.comparing(FullFeedEventDTO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));

            List<FullFeedEventDTO> combinedEvents = new ArrayList<>(eventsCreated);
            combinedEvents.addAll(eventsInvitedTo);

            return combinedEvents;
        } catch (Exception e) {
            logger.log(e.getMessage());
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }


    @Override
    public List<FullFeedEventDTO> getFilteredFeedEventsByFriendTagId(UUID friendTagFilterId) {
        try {
            UUID requestingUserId = friendTagService.getFriendTagById(friendTagFilterId).ownerUserId();
            List<FullFeedEventDTO> eventsCreated = convertEventsToFullFeedSelfOwnedEvents(getEventsByOwnerId(requestingUserId), requestingUserId);
            List<FullFeedEventDTO> eventsByFriendTagFilter = convertEventsToFullFeedEvents(getEventsByFriendTagId(friendTagFilterId), requestingUserId);

            // Combine the lists with eventsCreated first
            List<FullFeedEventDTO> combinedEvents = new ArrayList<>(eventsCreated);
            combinedEvents.addAll(eventsByFriendTagFilter);

            return combinedEvents;
        } catch (Exception e) {
            logger.log(e.getMessage());
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public FullFeedEventDTO getFullEventByEvent(EventDTO event, UUID requestingUserId, Set<UUID> visitedEvents) {
        try {
            if (visitedEvents.contains(event.id())) {
                return null;
            }
            visitedEvents.add(event.id());

            // Safely fetch location and creator
            LocationDTO location = event.locationId() != null
                    ? locationService.getLocationById(event.locationId())
                    : null;

            FullUserDTO creator = userService.getFullUserById(event.creatorUserId());

            return new FullFeedEventDTO(
                    event.id(),
                    event.title(),
                    event.startTime(),
                    event.endTime(),
                    location,
                    event.note(),
                    creator,
                    userService.convertUsersToFullUsers(userService.getParticipantsByEventId(event.id()), new HashSet<>()),
                    userService.convertUsersToFullUsers(userService.getInvitedByEventId(event.id()), new HashSet<>()),
                    null, // chatMessageService.getFullChatMessagesByEventId(event.id()),
                    "#ffffff", //requestingUserId != null ? getFriendTagColorHexCodeForRequestingUser(event, requestingUserId) : null,
                    ParticipationStatus.invited//requestingUserId != null ? getParticipationStatus(event.id(), requestingUserId) : null
            );
        } catch (BaseNotFoundException e) {
            System.err.println("Skipping event due to missing data: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String getFriendTagColorHexCodeForRequestingUser(EventDTO eventDTO, UUID requestingUserId) {
        // get event creator from eventDTO

        // use creator to get the friend tag that relates the requesting user to see
        // which friend tag they've placed them in
        FriendTagDTO pertainingFriendTag = friendTagService.getPertainingFriendTagByUserIds(requestingUserId, eventDTO.creatorUserId());

        // -> for now, we handle tie-breaks (user has same friend within two friend tags) in whichever way (just choose one)
        if (pertainingFriendTag == null) {
            return "#1D3D3D"; // Default color if no tag exists
        }
        // using that friend tag, grab its colorHexCode property to return from this method

        return pertainingFriendTag.colorHexCode();
    }

    @Override
    public List<FullFeedEventDTO> convertEventsToFullFeedEvents(List<EventDTO> events, UUID requestingUserId) {
        ArrayList<FullFeedEventDTO> fullEvents = new ArrayList<>();

        for (EventDTO eventDTO : events) {
            fullEvents.add(getFullEventByEvent(eventDTO, requestingUserId, new HashSet<>()));
        }

        return fullEvents;
    }

    @Override
    public List<FullFeedEventDTO> convertEventsToFullFeedSelfOwnedEvents(List<EventDTO> events, UUID requestingUserId) {
        ArrayList<FullFeedEventDTO> fullEvents = new ArrayList<>();

        for (EventDTO eventDTO : events) {
            FullFeedEventDTO fullFeedEvent = getFullEventByEvent(eventDTO, requestingUserId, new HashSet<>());
            fullFeedEvent.setEventFriendTagColorHexCodeForRequestingUser("#1D3D3D"); // from Figma & Mobile
            fullEvents.add(fullFeedEvent);
        }
        return fullEvents;
    }
}
