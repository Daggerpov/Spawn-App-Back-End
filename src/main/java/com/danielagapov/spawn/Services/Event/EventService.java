package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.Event.AbstractEventDTO;
import com.danielagapov.spawn.DTOs.Event.EventCreationDTO;
import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.Event.LocationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
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
import java.util.stream.Collectors;

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
        ArrayList<FullFeedEventDTO> fullEvents = new ArrayList<>();
        for (EventDTO e : getAllEvents()) {
            fullEvents.add(getFullEventByEvent(e, null, new HashSet<>()));
        }
        return fullEvents;
    }

    @Override
    public List<EventDTO> getAllEvents() {
        try {
            List<Event> events = repository.findAll();
            return getEventDTOs(events);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BasesNotFoundException(EntityType.Event);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public EventDTO getEventById(UUID id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, id));

        UUID creatorUserId = event.getCreator().getId();
        List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(id);
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(id);
        List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(id);

        return EventMapper.toDTO(event, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
    }

    @Override
    public FullFeedEventDTO getFullEventById(UUID id, UUID requestingUserId) {
        return getFullEventByEvent(getEventById(id), requestingUserId, new HashSet<>());
    }

    @Override
    public List<EventDTO> getEventsByFriendTagId(UUID tagId) {
        try {
            // Step 1: Retrieve the FriendTagDTO and its associated friend user IDs
            FriendTagDTO friendTag = friendTagService.getFriendTagById(tagId);
            List<UUID> friendIds = friendTag.getFriendUserIds();

            // Step 2: Retrieve events created by any of the friends
            // Step 3: Filter events based on whether their owner is in the list of friend
            // IDs
            List<Event> filteredEvents = repository.findByCreatorIdIn(friendIds);

            // Step 3: Map filtered events to detailed DTOs
            return filteredEvents.stream()
                    .map(event -> EventMapper.toDTO(
                            event,
                            event.getCreator().getId(),
                            userService.getParticipantUserIdsByEventId(event.getId()),
                            userService.getInvitedUserIdsByEventId(event.getId()),
                            chatMessageService.getChatMessageIdsByEventId(event.getId())))
                    .toList();
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new RuntimeException("Error retrieving events by friend tag ID", e);
        } catch (BaseNotFoundException e) {
            logger.log(e.getMessage());
            throw e; // Rethrow if it's a custom not-found exception
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public AbstractEventDTO saveEvent(AbstractEventDTO event) {
        try {
            Event eventEntity;

            if (event instanceof FullFeedEventDTO fullFeedEventDTO) {
                eventEntity = EventMapper.convertFullFeedEventDTOToEventEntity(fullFeedEventDTO);
            } else if (event instanceof EventDTO eventDTO) {
                Location location = locationRepository.findById(eventDTO.getLocationId()).orElse(null);

                // Map EventDTO to Event entity with the resolved Location
                eventEntity = EventMapper.toEntity(eventDTO, location,
                        userService.getUserEntityById(eventDTO.getCreatorUserId()));
            } else {
                throw new IllegalArgumentException("Unsupported event type");
            }

            // Save the Event entity
            eventEntity = repository.save(eventEntity);

            // Map saved Event entity back to EventDTO with all necessary fields
            // creatorUserId
            // participantUserIds
            // invitedUserIds
            // chatMessageIds
            return EventMapper.toDTO(
                    eventEntity,
                    eventEntity.getCreator().getId(), // creatorUserId
                    userService.getParticipantUserIdsByEventId(eventEntity.getId()), // participantUserIds
                    userService.getInvitedUserIdsByEventId(eventEntity.getId()), // invitedUserIds
                    chatMessageService.getChatMessageIdsByEventId(eventEntity.getId()) // chatMessageIds
            );
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save event: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public AbstractEventDTO createEvent(EventCreationDTO eventCreationDTO) {
        try {
            Location location = locationService.save(LocationMapper.toEntity(eventCreationDTO.getLocation()));

            User creator = userRepository.findById(eventCreationDTO.getCreatorUserId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, eventCreationDTO.getCreatorUserId()));

            Event event = EventMapper.fromCreationDTO(eventCreationDTO, location, creator);

            event = repository.save(event);

            Set<UUID> allInvitedUserIds = new HashSet<>();
            if (eventCreationDTO.getInvitedFriendTagIds() != null) {
                for (UUID friendTagId : eventCreationDTO.getInvitedFriendTagIds()) {
                    List<UUID> friendIdsForTag = userService.getFriendUserIdsByFriendTagId(friendTagId);
                    allInvitedUserIds.addAll(friendIdsForTag);
                }
            }
            if (eventCreationDTO.getInvitedFriendUserIds() != null) {
                allInvitedUserIds.addAll(eventCreationDTO.getInvitedFriendUserIds());
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
            }

            return EventMapper.toDTO(event, creator.getId(), null, new ArrayList<>(allInvitedUserIds), null);
        } catch (Exception e) {
            logger.log("Error creating event: " + e.getMessage());
            throw new ApplicationException("Failed to create event", e);
        }
    }

    @Override
    public List<EventDTO> getEventsByOwnerId(UUID creatorUserId) {
        List<Event> events = repository.findByCreatorId(creatorUserId);

        return getEventDTOs(events);
    }

    private List<EventDTO> getEventDTOs(List<Event> events) {
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

        return eventDTOs;
    }

    @Override
    public EventDTO replaceEvent(EventDTO newEvent, UUID id) {
        return repository.findById(id).map(event -> {
            // Update basic event details
            event.setTitle(newEvent.getTitle());
            event.setNote(newEvent.getNote());
            event.setEndTime(newEvent.getEndTime());
            event.setStartTime(newEvent.getStartTime());

            // Fetch the location entity by locationId from DTO
            event.setLocation(locationService.getLocationEntityById(newEvent.getLocationId()));

            // Save updated event
            repository.save(event);

            return constructDTOFromEntity(event);
        }).orElseGet(() -> {
            // Map and save new event, fetch location and creator
            Location location = locationService.getLocationEntityById(newEvent.getLocationId());
            User creator = userService.getUserEntityById(newEvent.getCreatorUserId());

            // Convert DTO to entity
            Event eventEntity = EventMapper.toEntity(newEvent, location, creator);
            repository.save(eventEntity);

            return constructDTOFromEntity(eventEntity);
        });
    }

    private EventDTO constructDTOFromEntity(Event eventEntity) {
        // Fetch related data for DTO
        UUID creatorUserId = eventEntity.getCreator().getId();
        List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(eventEntity.getId());
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(eventEntity.getId());
        List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(eventEntity.getId());

        return EventMapper.toDTO(eventEntity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
    }

    @Override
    public boolean deleteEventById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.Event, id);
        }

        try {
            repository.deleteById(id);
            return true;
        } catch (Exception e) {
            logger.log(e.getMessage());
            return false;
        }
    }

    @Override
    public List<UserDTO> getParticipatingUsersByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);

            if (eventUsers.isEmpty()) {
                throw new BaseNotFoundException(EntityType.Event, eventId);
            }

            return eventUsers.stream()
                    .filter(eventUser -> eventUser.getStatus().equals(ParticipationStatus.participating))
                    .map(eventUser -> userService.getUserById(eventUser.getUser().getId()))
                    .toList();
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseNotFoundException(EntityType.Event, eventId);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public ParticipationStatus getParticipationStatus(UUID eventId, UUID userId) {
        EventUsersId compositeId = new EventUsersId(eventId, userId);
        return eventUserRepository.findById(compositeId)
                .map(EventUser::getStatus)
                .orElse(ParticipationStatus.notInvited);
    }


    // return type boolean represents whether the user was already invited or not
    // if false -> invites them
    // if true -> return 400 in Controller to indicate that the user has already
    // been invited, or it is a bad request.
    @Override
    public boolean inviteUser(UUID eventId, UUID userId) {
        EventUsersId compositeId = new EventUsersId(eventId, userId);
        Optional<EventUser> existingEventUser = eventUserRepository.findById(compositeId);

        if (existingEventUser.isPresent()) {
            // User is already invited
            return existingEventUser.get().getStatus().equals(ParticipationStatus.invited);
        } else {
            // Create a new invitation.
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
            Event event = repository.findById(eventId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, eventId));

            EventUser newEventUser = new EventUser();
            newEventUser.setId(compositeId);
            newEventUser.setEvent(event);
            newEventUser.setUser(user);
            newEventUser.setStatus(ParticipationStatus.invited);

            eventUserRepository.save(newEventUser);
            return false;
        }
    }


    // returns the updated event, with modified participants and invited users
    // invited/participating
    // if true -> change status
    // if false -> return 400 in controller to indicate that the user is not
    // invited/participating
    @Override
    public FullFeedEventDTO toggleParticipation(UUID eventId, UUID userId) {
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
                break;
            }
        }
        return getFullEventById(eventId, userId);
    }

    @Override
    public List<EventDTO> getEventsInvitedTo(UUID id) {
        List<EventUser> eventUsers = eventUserRepository.findByUser_Id(id);

        List<Event> events = new ArrayList<>();

        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(id)) {
                events.add(eventUser.getEvent());
            }
        }

        return getEventDTOs(events);
    }

    @Override
    public List<FullFeedEventDTO> getFullEventsInvitedTo(UUID id) {
        List<EventUser> eventUsers = eventUserRepository.findByUser_Id(id);

        if (eventUsers == null || eventUsers.isEmpty()) {
            return Collections.emptyList(); // ✅ Always return an empty list instead of null
        }

        List<Event> events = new ArrayList<>();

        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(id) && eventUser.getStatus() != ParticipationStatus.notInvited) {
                events.add(eventUser.getEvent());
            }
        }

        List<EventDTO> eventDTOs = getEventDTOs(events);

        return eventDTOs.stream()
                .map(eventDTO -> getFullEventByEvent(eventDTO, id, new HashSet<>()))
                .toList();
    }

    /**
     * @param requestingUserId this is the user whose feed is being loaded
     * @return This method returns the feed events for a user, with their created ones
     * first in the `universalAccentColor`, followed by events they're invited to
     */
    @Override
    public List<FullFeedEventDTO> getFeedEvents(UUID requestingUserId) {
        try {
            // Retrieve events created by the user.
            List<FullFeedEventDTO> eventsCreated = convertEventsToFullFeedSelfOwnedEvents(
                    getEventsByOwnerId(requestingUserId),
                    requestingUserId
            );

            List<FullFeedEventDTO> eventsInvitedTo = getFullEventsInvitedTo(requestingUserId);

            // Remove expired events
            eventsCreated = removeExpiredEvents(eventsCreated);
            eventsInvitedTo = removeExpiredEvents(eventsInvitedTo);

            // Sort events
            sortEventsByStartTime(eventsCreated);
            sortEventsByStartTime(eventsInvitedTo);

            // Combine the two lists into one.
            List<FullFeedEventDTO> combinedEvents = new ArrayList<>(eventsCreated);
            combinedEvents.addAll(eventsInvitedTo);
            return combinedEvents;
        } catch (Exception e) {
            logger.log("Error fetching feed events for user: " + requestingUserId + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Removes expired events from the provided list, and returns it modified.
     * An event is considered expired if its end time is set and is before the current time.
     *
     * @param events the list of events to filter
     * @return the modified list
     */
    private List<FullFeedEventDTO> removeExpiredEvents(List<FullFeedEventDTO> events) {
        OffsetDateTime now = OffsetDateTime.now();

        if (events == null) {
            return Collections.emptyList();
        }

        return events.stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getEndTime() == null || !event.getEndTime().isBefore(now))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Sorts a list of events by their start time, keeping null values at the end.
     *
     * @param events the list of events to sort
     */
    private void sortEventsByStartTime(List<FullFeedEventDTO> events) {
        events.sort(Comparator.comparing(FullFeedEventDTO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    @Override
    public List<FullFeedEventDTO> getFilteredFeedEventsByFriendTagId(UUID friendTagFilterId) {
        try {
            UUID requestingUserId = friendTagService.getFriendTagById(friendTagFilterId).getOwnerUserId();
            List<FullFeedEventDTO> eventsCreated = convertEventsToFullFeedSelfOwnedEvents(getEventsByOwnerId(requestingUserId), requestingUserId);
            List<FullFeedEventDTO> eventsByFriendTagFilter = convertEventsToFullFeedEvents(getEventsByFriendTagId(friendTagFilterId), requestingUserId);

            // Remove expired events
            removeExpiredEvents(eventsCreated);
            removeExpiredEvents(eventsByFriendTagFilter);

            // Sort events
            sortEventsByStartTime(eventsCreated);
            sortEventsByStartTime(eventsByFriendTagFilter);

            // Combine the lists with eventsCreated first.
            List<FullFeedEventDTO> combinedEvents = new ArrayList<>(eventsCreated);
            combinedEvents.addAll(eventsByFriendTagFilter);
            return combinedEvents;
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public FullFeedEventDTO getFullEventByEvent(EventDTO event, UUID requestingUserId, Set<UUID> visitedEvents) {
        try {
            if (visitedEvents.contains(event.getId())) {
                return null;
            }
            visitedEvents.add(event.getId());

            // Safely fetch location and creator
            LocationDTO location = event.getLocationId() != null
                    ? locationService.getLocationById(event.getLocationId())
                    : null;

            UserDTO creator = userService.getUserById(event.getCreatorUserId());

            return new FullFeedEventDTO(
                    event.getId(),
                    event.getTitle(),
                    event.getStartTime(),
                    event.getEndTime(),
                    location,
                    event.getNote(),
                    creator,
                    userService.getParticipantsByEventId(event.getId()),
                    userService.getInvitedByEventId(event.getId()),
                    chatMessageService.getFullChatMessagesByEventId(event.getId()),
                    requestingUserId != null ? getFriendTagColorHexCodeForRequestingUser(event, requestingUserId) : null,
                    requestingUserId != null ? getParticipationStatus(event.getId(), requestingUserId) : null,
                    requestingUserId == event.getCreatorUserId()
            );
        } catch (BaseNotFoundException e) {
            return null;
        }
    }

    @Override
    public String getFriendTagColorHexCodeForRequestingUser(EventDTO eventDTO, UUID requestingUserId) {
        // get event creator from eventDTO

        // use creator to get the friend tag that relates the requesting user to see
        // which friend tag they've placed them in
        return Optional.ofNullable(friendTagService.getPertainingFriendTagBetweenUsers(requestingUserId, eventDTO.getCreatorUserId()))
                .flatMap(optional -> optional)  // This will flatten the Optional<Optional<FriendTagDTO>> to Optional<FriendTagDTO>
                .map(FriendTagDTO::getColorHexCode)
                .orElse("#8693FF"); // Default color if no tag exists or if result is null
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

            if (fullFeedEvent == null) {
                continue;
            }

            // Apply universal accent color
            fullFeedEvent.setEventFriendTagColorHexCodeForRequestingUser("#8693FF");

            fullEvents.add(fullFeedEvent);
        }

        return fullEvents;
    }

}
