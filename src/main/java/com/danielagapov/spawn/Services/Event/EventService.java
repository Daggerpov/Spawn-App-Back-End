package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Mappers.EventMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventService implements IEventService {
    private final IEventRepository repository;
    private final ILocationRepository locationRepository;
    private final IFriendTagService friendTagService;
    private final IUserService userService;
    private final IChatMessageService chatMessageService;
    private final ILogger logger;

    public EventService(IEventRepository repository, ILocationRepository locationRepository,
            IFriendTagService friendTagService, IUserService userService, IChatMessageService chatMessageService,
            ILogger logger) {
        this.repository = repository;
        this.locationRepository = locationRepository;
        this.friendTagService = friendTagService;
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.logger = logger;
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

        UUID creatorUserId =event.getCreator().getId();
        List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(id);
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(id);
        List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(id);

        return EventMapper.toDTO(event, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
    }

    public List<EventDTO> getEventsByFriendTagId(UUID tagId) {
        try {
            // Step 1: Retrieve the FriendTagDTO and its associated friend user IDs
            FriendTagDTO friendTag = friendTagService.getFriendTagById(tagId);
            List<UUID> friendIds = friendTag.friendUserIds();  // Use friendUserIds instead of friends

            if (friendIds.isEmpty()) {
                return List.of();  // Return an empty list if there are no friends
            }

            // Step 2: Retrieve events created by any of the friends
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
            throw new RuntimeException("Unexpected error", e);  // Generic error handling
        }
    }


    public EventDTO saveEvent(EventDTO event) {
        try {
            Location location = locationRepository.findById(event.locationId()).orElse(null);

            // Map EventDTO to Event entity with the resolved Location
            Event eventEntity = EventMapper.toEntity(event, location, userService.getUserEntityById(event.creatorUserId()));

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
            EventDTO eventDTO = EventMapper.toDTO(event, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
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

            // Handle location
            Location location = LocationMapper.toEntity(newEvent.location());
            if (location.getId() != null) {
                location = locationRepository.findById(location.getId())
                        .orElse(locationRepository.save(location));
            } else {
                location = locationRepository.save(location);
            }
            event.setLocation(location);

            // Save updated event
            repository.save(event);

            // Fetch related data for DTO
            UUID creatorUserId = event.getCreator().getId();
            List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(event.getId());
            List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(event.getId());
            List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(event.getId());

            return EventMapper.toDTO(event, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
        }).orElseGet(() -> {
            // Handle location for new event
            Location location = LocationMapper.toEntity(newEvent.location());
            if (location.getId() != null) {
                location = locationRepository.findById(location.getId())
                        .orElse(locationRepository.save(location));
            } else {
                location = locationRepository.save(location);
            }

            // Map and save new event
            Event eventEntity = EventMapper.toEntity(newEvent, location);
            repository.save(eventEntity);

            // Fetch related data for DTO
            UUID creatorUserId = eventEntity.getCreator().getId();
            List<UUID> participantUserIds = userService.getParticipantUserIdsByEventId(eventEntity.getId());
            List<UUID> invitedUserIds = userService.getInvitedUserIdsByEventId(eventEntity.getId());
            List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByEventId(eventEntity.getId());

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
}
