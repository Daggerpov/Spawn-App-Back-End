package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.*;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.EventUser;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public List<EventDTO> getAllEvents() {
        try {
            List<Event> events = repository.findAll();
            return getEventDTOS(events);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BasesNotFoundException(EntityType.Event);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    public EventDTO getEventById(UUID id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, id));

        UUID creatorUserId = event.getCreator().getId();
        List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(id);
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(id);
        List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(id);

        return EventMapper.toDTO(event, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
    }

    public FullEventDTO getFullEventById(UUID id) {
        return getFullEventByEvent(getEventById(id));
    }

    public List<EventDTO> getEventsByFriendTagId(UUID tagId) {
        try {
            // Step 1: Retrieve the FriendTagDTO and its associated friend user IDs
            FriendTagDTO friendTag = friendTagService.getFriendTagById(tagId);
            List<UUID> friendIds = friendTag.friendUserIds(); // Use friendUserIds instead of friends

            if (friendIds.isEmpty()) {
                return List.of(); // Return an empty list if there are no friends
            }

            // Step 2: Retrieve events created by any of the friends
            // Step 3: Filter events based on whether their owner is in the list of friend
            // IDs
            List<Event> filteredEvents = repository.findByCreatorIdIn(friendIds);

            if (filteredEvents.isEmpty()) {
                throw new BasesNotFoundException(EntityType.Event);
            }

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
            throw new RuntimeException("Unexpected error", e); // Generic error handling
        }
    }

    public EventDTO saveEvent(EventDTO event) {
        try {
            Location location = locationRepository.findById(event.locationId()).orElse(null);

            // Map EventDTO to Event entity with the resolved Location
            Event eventEntity = EventMapper.toEntity(event, location,
                    userService.getUserEntityById(event.creatorUserId()));

            // Save the Event entity
            eventEntity = repository.save(eventEntity);

            // Map saved Event entity back to EventDTO with all necessary fields
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

    public List<EventDTO> getEventsByUserId(UUID userId) {
        List<Event> events = repository.findByCreatorId(userId);

        if (events.isEmpty()) {
            throw new BasesNotFoundException(EntityType.Event);
        }

        return getEventDTOS(events);
    }

    private List<EventDTO> getEventDTOS(List<Event> events) {
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

    public EventDTO replaceEvent(EventDTO newEvent, UUID id) {
        return repository.findById(id).map(event -> {
            // Update basic event details
            event.setTitle(newEvent.title());
            event.setNote(newEvent.note());
            event.setEndTime(newEvent.endTime());
            event.setStartTime(newEvent.startTime());

            // Fetch the location entity by locationId from DTO
            LocationDTO location = locationService.getLocationById(newEvent.locationId());
            event.setLocation(LocationMapper.toEntity(location));

            // Save updated event
            repository.save(event);

            // Fetch related data for DTO
            UUID creatorUserId = event.getCreator().getId();
            List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(event.getId());
            List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(event.getId());
            List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(event.getId());

            // Convert updated event to DTO
            return EventMapper.toDTO(event, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
        }).orElseGet(() -> {
            // Map and save new event, fetch location and creator
            Location location = LocationMapper.toEntity(locationService.getLocationById(newEvent.locationId()));
            User creator = userService.getUserEntityById(newEvent.creatorUserId());

            // Convert DTO to entity
            Event eventEntity = EventMapper.toEntity(newEvent, location, creator);
            repository.save(eventEntity);

            // Fetch related data for DTO
            UUID creatorUserId = eventEntity.getCreator().getId();
            List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(eventEntity.getId());
            List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(eventEntity.getId());
            List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(eventEntity.getId());

            // Return DTO after entity creation
            return EventMapper.toDTO(eventEntity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
        });
    }

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

    public List<UserDTO> getParticipatingUsersByEventId(UUID id) {
        return List.of();
    }

    public ParticipationStatus getParticipationStatus(UUID eventId, UUID userId) {
        if (!eventUserRepository.existsById(eventId)) {
            throw new BaseNotFoundException(EntityType.Event, eventId);
        }

        List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);
        if (eventUsers.isEmpty()) {
            throw new BaseNotFoundException(EntityType.Event, eventId);
        }

        for (EventUser eventUser: eventUsers) {
            if (eventUser.getUser().getId().equals(userId)) {
                return eventUser.getStatus();
            }
        }

        // if for loop doesnt find it, return notInvited
        return ParticipationStatus.notInvited;
    }

    // return type boolean represents whether the user was already invited or not
    // if false -> invites them
    // if true -> return 400 in Controller to indicate that the user has already
    // been invited or it is a bad request.
    public boolean inviteUser(UUID eventId, UUID userId) {
        List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);
        if (eventUsers.isEmpty()) {
            // throw BaseNotFound for events if eventId has no EventUsers
            throw new BaseNotFoundException(EntityType.Event, eventId);
        }

        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(userId)) {
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
                return false;
            }
        }
        // if the loop doesn't return, its a bad request
        return true;
    }

    // return type boolean represents whether the user was already
    // invited/participating
    // if true -> change status
    // if false -> return 400 in controller to indicate that the user is not
    // invited/participating
    public boolean toggleParticipation(UUID eventId, UUID userId) {
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
                    eventUserRepository.save(eventUser);
                } else if (eventUser.getStatus().equals(ParticipationStatus.participating)) {
                    eventUser.setStatus(ParticipationStatus.invited);
                }
            }
        }
        return false;
    }

    public List<EventDTO> getEventsInvitedTo(UUID id) {
        List<EventUser> eventUsers = eventUserRepository.findByUser_Id(id);

        List<Event> events = new ArrayList<>();

        if (events.isEmpty()) {
            // throws no user found exception
            throw new BaseNotFoundException(EntityType.User, id);
        }

        for (EventUser eventUser : eventUsers) {
            if (eventUser.getUser().getId().equals(id)) {
                events.add(eventUser.getEvent());
            }
        }

        return getEventDTOS(events);
    }

    public FullEventDTO getFullEventByEvent(EventDTO event) {
        return new FullEventDTO(
                event.id(),
                event.title(),
                event.startTime(),
                event.endTime(),
                locationService.getLocationById(event.locationId()),
                event.note(),
                userService.getUserById(event.creatorUserId()),
                userService.getParticipantsByEventId(event.id()),
                userService.getInvitedByEventId(event.id()),
                chatMessageService.getChatMessagesByEventId(event.id())
        );
    }
}
