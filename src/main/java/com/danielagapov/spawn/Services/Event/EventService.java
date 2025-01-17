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

    public EventService(IEventRepository repository, ILocationRepository locationRepository, IFriendTagService friendTagService, IUserService userService, IChatMessageService chatMessageService, ILogger logger) {
        this.repository = repository;
        this.locationRepository = locationRepository;
        this.friendTagService = friendTagService;
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.logger = logger;
    }

    public List<EventDTO> getAllEvents() {
        System.out.println("entered getallevents");
        try {
            List<Event> events = repository.findAll();
            System.out.println("entered try");
            return getEventDTOS(events);
        } catch (DataAccessException e) {
            System.out.println("entered catch");
            logger.log(e.getMessage());
            System.out.println("entered catch, after logger");
            throw new BasesNotFoundException(EntityType.Event);
        }
    }

    public EventDTO getEventById(UUID id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, id));

        UserDTO creator = userService.getUserById(event.getCreator().getId());
        List<UserDTO> participants = userService.getParticipantsByEventId(id);
        List<UserDTO> invited = userService.getInvitedByEventId(id);
        List<ChatMessageDTO> chatMessages = chatMessageService.getChatMessagesByEventId(id);

        return EventMapper.toDTO(event, creator, participants, invited, chatMessages);
    }

    public List<EventDTO> getEventsByFriendTagId(UUID tagId) {
        try {
            // Step 1: Retrieve the FriendTag and its associated friends
            FriendTagDTO friendTag = friendTagService.getFriendTagById(tagId);
            List<UserDTO> friends = friendTag.friends();

            if (friends.isEmpty()) {
                return List.of();
            }

            // Step 2: Collect all friend user IDs
            List<UUID> friendIds = friends.stream()
                    .map(UserDTO::id)
                    .toList();

            // Step 3: Retrieve events created by any of the friends
            List<Event> filteredEvents = repository.findByCreatorIdIn(friendIds);

            if (filteredEvents.isEmpty()) {
                throw new BasesNotFoundException(EntityType.Event);
            }

            // Step 4: Map filtered events to detailed DTOs
            return filteredEvents.stream()
                    .map(event -> EventMapper.toDTO(
                            event,
                            userService.getUserById(event.getCreator().getId()),
                            userService.getParticipantsByEventId(event.getId()),
                            userService.getInvitedByEventId(event.getId()),
                            chatMessageService.getChatMessagesByEventId(event.getId())
                    ))
                    .toList();
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new RuntimeException("Error retrieving events by tag ID", e);
        }
    }

    public EventDTO saveEvent(EventDTO event) {
        try {
            // Convert LocationDTO to Location entity and handle save or resolve
            Location location = LocationMapper.toEntity(event.location());
            Location finalLocation = location;
            location = (location.getId() != null)
                    ? locationRepository.findById(location.getId())
                    .orElseGet(() -> locationRepository.save(finalLocation))
                    : locationRepository.save(location);

            // Map EventDTO to Event entity with the resolved Location
            Event eventEntity = EventMapper.toEntity(event, location);

            // Save the Event entity
            eventEntity = repository.save(eventEntity);

            // Map saved Event entity back to EventDTO with all necessary fields
            return EventMapper.toDTO(
                    eventEntity,
                    userService.getUserById(eventEntity.getCreator().getId()),
                    userService.getParticipantsByEventId(eventEntity.getId()),
                    userService.getInvitedByEventId(eventEntity.getId()),
                    chatMessageService.getChatMessagesByEventId(eventEntity.getId())
            );
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save event: " + e.getMessage());
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
            UserDTO creator = userService.getUserById(event.getCreator().getId());
            List<UserDTO> participants = userService.getParticipantsByEventId(eventId);
            List<UserDTO> invited = userService.getInvitedByEventId(eventId);
            List<ChatMessageDTO> chatMessages = chatMessageService.getChatMessagesByEventId(eventId);

            // Map the event to its DTO
            EventDTO eventDTO = EventMapper.toDTO(event, creator, participants, invited, chatMessages);
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
            UserDTO creator = userService.getUserById(event.getCreator().getId());
            List<UserDTO> participants = userService.getParticipantsByEventId(event.getId());
            List<UserDTO> invited = userService.getInvitedByEventId(event.getId());
            List<ChatMessageDTO> chatMessages = chatMessageService.getChatMessagesByEventId(event.getId());

            return EventMapper.toDTO(event, creator, participants, invited, chatMessages);
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
            UserDTO creator = userService.getUserById(eventEntity.getCreator().getId());
            List<UserDTO> participants = userService.getParticipantsByEventId(eventEntity.getId());
            List<UserDTO> invited = userService.getInvitedByEventId(eventEntity.getId());
            List<ChatMessageDTO> chatMessages = chatMessageService.getChatMessagesByEventId(eventEntity.getId());

            return EventMapper.toDTO(eventEntity, creator, participants, invited, chatMessages);
        });
    }


    public boolean deleteEventById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.Event, id);
        }

        try {
            repository.deleteById(id);
            return true;
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            return false;
        }
    }

    public List<UserDTO> getParticipatingUsersByEventId(UUID id) {
        return List.of();
    }
}
