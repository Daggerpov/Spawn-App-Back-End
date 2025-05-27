package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.Event.*;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
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
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.Location.ILocationService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.danielagapov.spawn.Events.EventInviteNotificationEvent;
import com.danielagapov.spawn.Events.EventParticipationNotificationEvent;
import com.danielagapov.spawn.Events.EventUpdateNotificationEvent;

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
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    @Lazy // avoid circular dependency problems with ChatMessageService
    public EventService(IEventRepository repository, ILocationRepository locationRepository,
                        IEventUserRepository eventUserRepository, IUserRepository userRepository,
                        IFriendTagService friendTagService, IUserService userService, IChatMessageService chatMessageService,
                        ILogger logger, ILocationService locationService, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.locationRepository = locationRepository;
        this.eventUserRepository = eventUserRepository;
        this.userRepository = userRepository;
        this.friendTagService = friendTagService;
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.logger = logger;
        this.locationService = locationService;
        this.eventPublisher = eventPublisher;
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
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.Event);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "eventById", key = "#id")
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
    @Cacheable(value = "fullEventById", key = "#id.toString() + ':' + #requestingUserId.toString()")
    public FullFeedEventDTO getFullEventById(UUID id, UUID requestingUserId) {
        return getFullEventByEvent(getEventById(id), requestingUserId, new HashSet<>());
    }

    @Override
    @Cacheable(value = "eventsByFriendTagId", key = "#tagId")
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
            logger.error(e.getMessage());
            throw new RuntimeException("Error retrieving events by friend tag ID", e);
        } catch (BaseNotFoundException e) {
            logger.error(e.getMessage());
            throw e; // Rethrow if it's a custom not-found exception
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "eventById", key = "#result.id"),
            @CacheEvict(value = "fullEventById", allEntries = true),
            @CacheEvict(value = "eventsByOwnerId", key = "#result.creatorUserId"),
            @CacheEvict(value = "feedEvents", allEntries = true),
            @CacheEvict(value = "filteredFeedEvents", allEntries = true)
    })
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
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save event: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "eventById", key = "#result.id"),
            @CacheEvict(value = "fullEventById", allEntries = true),
            @CacheEvict(value = "eventsByOwnerId", key = "#result.creatorUserId"),
            @CacheEvict(value = "feedEvents", allEntries = true),
            @CacheEvict(value = "filteredFeedEvents", allEntries = true)
    })
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

            // Create and publish event invite notification directly
            eventPublisher.publishEvent(
                new EventInviteNotificationEvent(event.getCreator(), event, allInvitedUserIds)
            );

            return EventMapper.toDTO(event, creator.getId(), null, new ArrayList<>(allInvitedUserIds), null);
        } catch (Exception e) {
            logger.error("Error creating event: " + e.getMessage());
            throw new ApplicationException("Failed to create event", e);
        }
    }

    @Override
    @Cacheable(value = "eventsByOwnerId", key = "#creatorUserId")
    public List<EventDTO> getEventsByOwnerId(UUID creatorUserId) {
        List<Event> events = repository.findByCreatorId(creatorUserId);
        return getEventDTOs(events);
    }

    private List<EventDTO> getEventDTOs(List<Event> events) {
        return events.stream()
                .map(event -> EventMapper.toDTO(
                        event,
                        event.getCreator().getId(),
                        userService.getParticipantUserIdsByEventId(event.getId()),
                        userService.getInvitedUserIdsByEventId(event.getId()),
                        chatMessageService.getChatMessageIdsByEventId(event.getId())))
                .toList();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "eventById", key = "#result.id"),
            @CacheEvict(value = "fullEventById", allEntries = true),
            @CacheEvict(value = "eventsByOwnerId", key = "#result.creatorUserId"),
            @CacheEvict(value = "feedEvents", allEntries = true),
            @CacheEvict(value = "filteredFeedEvents", allEntries = true)
    })
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

            eventPublisher.publishEvent(
                new EventUpdateNotificationEvent(event.getCreator(), event, eventUserRepository)
            );
            return constructDTOFromEntity(event);
        }).orElseGet(() -> {
            // Map and save new event, fetch location and creator
            Location location = locationService.getLocationEntityById(newEvent.getLocationId());
            User creator = userService.getUserEntityById(newEvent.getCreatorUserId());

            // Convert DTO to entity
            Event eventEntity = EventMapper.toEntity(newEvent, location, creator);
            eventEntity = repository.save(eventEntity);
            
            eventPublisher.publishEvent(
                new EventUpdateNotificationEvent(eventEntity.getCreator(), eventEntity, eventUserRepository)
            );
            return constructDTOFromEntity(eventEntity);
        });
    }

    private List<UUID> getParticipatingUserIdsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findEventsByEvent_IdAndStatus(eventId, ParticipationStatus.participating);
            return eventUsers.stream().map((eventUser -> eventUser.getUser().getId())).collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding events by event id: " + e.getMessage());
            throw e;
        }
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
    @Caching(evict = {
            @CacheEvict(value = "eventById", key = "#result.id"),
            @CacheEvict(value = "fullEventById", allEntries = true),
            @CacheEvict(value = "eventsByOwnerId", key = "#result.creatorUserId"),
            @CacheEvict(value = "feedEvents", allEntries = true),
            @CacheEvict(value = "filteredFeedEvents", allEntries = true)
    })
    public boolean deleteEventById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.Event, id);
        }

        try {
            repository.deleteById(id);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public List<UserDTO> getParticipatingUsersByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_IdAndStatus(eventId, ParticipationStatus.participating);
            return eventUsers.stream()
                    .map(eventUser -> userService.getUserById(eventUser.getUser().getId()))
                    .toList();
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseNotFoundException(EntityType.Event, eventId);
        } catch (Exception e) {
            logger.error(e.getMessage());
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
    @Caching(evict = {
            @CacheEvict(value = "eventsInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullEventsInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullEventById", key = "#eventId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "feedEvents", key = "#userId"),
            @CacheEvict(value = "filteredFeedEvents", key = "#userId")
    })
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
    @Caching(evict = {
            @CacheEvict(value = "eventsInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullEventsInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullEventById", key = "#eventId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "feedEvents", key = "#userId"),
            @CacheEvict(value = "filteredFeedEvents", key = "#userId")
    })
    public FullFeedEventDTO toggleParticipation(UUID eventId, UUID userId) {
        EventUser eventUser = eventUserRepository.findByEvent_IdAndUser_Id(eventId, userId).orElseThrow(() -> new BaseNotFoundException(EntityType.EventUser));

        if (eventUser.getStatus() == ParticipationStatus.participating) {
            eventUser.setStatus(ParticipationStatus.invited);
        } else if (eventUser.getStatus().equals(ParticipationStatus.invited)) {
            eventUser.setStatus(ParticipationStatus.participating);
        }
        
        final Event event = eventUser.getEvent();
        final User user = eventUser.getUser();
        final ParticipationStatus status = eventUser.getStatus();
        
        if (status == ParticipationStatus.participating) { // Status changed from invited to participating
            eventPublisher.publishEvent(
                EventParticipationNotificationEvent.forJoining(user, event)
            );
        } else if (status == ParticipationStatus.invited) { // Status changed from participating to invited
            eventPublisher.publishEvent(
                EventParticipationNotificationEvent.forLeaving(user, event)
            );
        }
        
        eventUserRepository.save(eventUser);
        return getFullEventById(eventId, userId);
    }

    @Override
    @Cacheable(value = "eventsInvitedTo", key = "#id")
    public List<EventDTO> getEventsInvitedTo(UUID id) {
        List<EventUser> eventUsers = eventUserRepository.findByUser_IdAndStatus(id, ParticipationStatus.invited);
        return getEventDTOs(eventUsers.stream()
                .map(EventUser::getEvent)
                .toList());
    }

    @Override
    @Cacheable(value = "eventsInvitedToByFriendTagId", key = "#friendTagId.toString() + ':' + #requestingUserId")
    public List<EventDTO> getEventsInvitedToByFriendTagId(UUID friendTagId, UUID requestingUserId) {
        try {
            List<Event> events = repository.getEventsInvitedToWithFriendTagId(friendTagId, requestingUserId);
            return getEventDTOs(events);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseNotFoundException(EntityType.FriendTag, friendTagId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "fullEventsInvitedTo", key = "#id")
    public List<FullFeedEventDTO> getFullEventsInvitedTo(UUID id) {
        List<EventUser> eventUsers = eventUserRepository.findByUser_IdAndStatus(id, ParticipationStatus.invited);
        return convertEventsToFullFeedEvents(
                getEventDTOs(eventUsers.stream()
                        .map(EventUser::getEvent)
                        .toList()),
                id);
    }

    /**
     * @param requestingUserId this is the user whose feed is being loaded
     * @return This method returns the feed events for a user, with their created ones
     * first in the `universalAccentColor`, followed by events they're invited to
     */
    @Override
    @Cacheable(value = "feedEvents", key = "#requestingUserId")
    public List<FullFeedEventDTO> getFeedEvents(UUID requestingUserId) {
        try {
            // Retrieve events created by the user.
            List<FullFeedEventDTO> eventsCreated = convertEventsToFullFeedSelfOwnedEvents(
                    getEventsByOwnerId(requestingUserId),
                    requestingUserId
            );

            List<FullFeedEventDTO> eventsInvitedTo = getFullEventsInvitedTo(requestingUserId);

            return makeFeed(eventsCreated, eventsInvitedTo);
        } catch (Exception e) {
            logger.error("Error fetching feed events for user: " + requestingUserId + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Helper function to remove expired events, sort by time, and combine the events created by a user,
     * and the events they are invited to
     */
    private List<FullFeedEventDTO> makeFeed(List<FullFeedEventDTO> eventsCreated, List<FullFeedEventDTO> eventsInvitedTo) {
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
    @Cacheable(value = "filteredFeedEvents", key = "#friendTagFilterId")
    public List<FullFeedEventDTO> getFilteredFeedEventsByFriendTagId(UUID friendTagFilterId) {
        try {
            UUID requestingUserId = friendTagService.getFriendTagById(friendTagFilterId).getOwnerUserId();
            List<FullFeedEventDTO> eventsCreated = convertEventsToFullFeedSelfOwnedEvents(getEventsByOwnerId(requestingUserId), requestingUserId);
            List<FullFeedEventDTO> eventsByFriendTagFilter = convertEventsToFullFeedEvents(getEventsInvitedToByFriendTagId(friendTagFilterId, requestingUserId), requestingUserId);

            // Remove expired events and sort
            return makeFeed(eventsCreated, eventsByFriendTagFilter);
        } catch (Exception e) {
            logger.error(e.getMessage());
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
                    event.getIcon(),
                    event.getCategory(),
                    creator,
                    userService.getParticipantsByEventId(event.getId()),
                    userService.getInvitedByEventId(event.getId()),
                    chatMessageService.getFullChatMessagesByEventId(event.getId()),
                    requestingUserId != null ? getFriendTagColorHexCodeForRequestingUser(event, requestingUserId) : null,
                    requestingUserId != null ? getParticipationStatus(event.getId(), requestingUserId) : null,
                    event.getCreatorUserId().equals(requestingUserId),
                    event.getCreatedAt()
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

    @Override
    public Instant getLatestCreatedEventTimestamp(UUID userId) {
        try {
            return repository.findTopByCreatorIdOrderByLastUpdatedDesc(userId)
                    .map(Event::getLastUpdated)
                    .orElse(null);
        } catch (DataAccessException e) {
            logger.error("Error fetching latest created event timestamp for user: " + userId + " - " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Instant getLatestInvitedEventTimestamp(UUID userId) {
        try {
            return eventUserRepository.findTopByUserIdAndStatusOrderByEventLastUpdatedDesc(userId, ParticipationStatus.invited)
                    .map(eventUser -> eventUser.getEvent().getLastUpdated())
                    .orElse(null);
        } catch (DataAccessException e) {
            logger.error("Error fetching latest invited event timestamp for user: " + userId + " - " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Instant getLatestUpdatedEventTimestamp(UUID userId) {
        try {
            return eventUserRepository.findTopByUserIdAndStatusOrderByEventLastUpdatedDesc(userId, ParticipationStatus.participating)
                    .map(eventUser -> eventUser.getEvent().getLastUpdated())
                    .orElse(null);
        } catch (DataAccessException e) {
            logger.error("Error fetching latest updated event timestamp for user: " + userId + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Gets past events where the specified user invited the requesting user
     * 
     * @param inviterUserId The user ID of the person who invited the requesting user
     * @param requestingUserId The user ID of the user viewing the profile
     * @return List of past events where inviterUserId invited requestingUserId
     */
    @Override
    public List<ProfileEventDTO> getPastEventsWhereUserInvited(UUID inviterUserId, UUID requestingUserId) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            List<Event> pastEvents = repository.getPastEventsWhereUserInvited(inviterUserId, requestingUserId, now);
            List<EventDTO> pastEventDTOs = getEventDTOs(pastEvents);
            
            // Convert to FullFeedEventDTOs then to ProfileEventDTOs and mark them as past events
            List<FullFeedEventDTO> fullFeedEvents = convertEventsToFullFeedEvents(pastEventDTOs, requestingUserId);
            List<ProfileEventDTO> result = new ArrayList<>();
            
            // Convert each FullFeedEventDTO to ProfileEventDTO with isPastEvent set to true
            for (FullFeedEventDTO fullFeedEvent : fullFeedEvents) {
                result.add(ProfileEventDTO.fromFullFeedEventDTO(fullFeedEvent, true));
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error fetching past events where user " + inviterUserId + " invited user " + requestingUserId + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Gets feed events for a profile. If the profile user has no upcoming events, returns past events
     * that the profile user invited the requesting user to, with a flag indicating they are past events.
     *
     * @param profileUserId The user ID of the profile being viewed
     * @param requestingUserId The user ID of the user viewing the profile
     * @return List of events with a flag indicating if they are past events
     */
    @Override
    public List<ProfileEventDTO> getProfileEvents(UUID profileUserId, UUID requestingUserId) {
        try {
            // Get upcoming events created by the profile user
            List<EventDTO> upcomingEvents = getEventsByOwnerId(profileUserId);
            List<FullFeedEventDTO> upcomingFullEvents = convertEventsToFullFeedSelfOwnedEvents(upcomingEvents, requestingUserId);
            
            // Remove expired events
            List<FullFeedEventDTO> nonExpiredEvents = removeExpiredEvents(upcomingFullEvents);
            
            // Convert to ProfileEventDTO
            List<ProfileEventDTO> result = new ArrayList<>();
            
            // If there are upcoming events, return them as ProfileEventDTOs with isPastEvent = false
            if (!nonExpiredEvents.isEmpty()) {
                sortEventsByStartTime(nonExpiredEvents);
                for (FullFeedEventDTO event : nonExpiredEvents) {
                    result.add(ProfileEventDTO.fromFullFeedEventDTO(event, false));
                }
                return result;
            }
            
            // If no upcoming events, get past events where the profile user invited the requesting user
            return getPastEventsWhereUserInvited(profileUserId, requestingUserId);
        } catch (Exception e) {
            logger.error("Error fetching profile events for user " + profileUserId + 
                         " requested by " + requestingUserId + ": " + e.getMessage());
            throw e;
        }
    }

}
