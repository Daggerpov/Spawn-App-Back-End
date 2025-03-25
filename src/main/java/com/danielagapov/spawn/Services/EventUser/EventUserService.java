package com.danielagapov.spawn.Services.EventUser;

import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Events.EventParticipationNotificationEvent;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.CompositeKeys.EventUsersId;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.User.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class EventUserService implements IEventUserService {
    private final IEventUserRepository repository;
    private final IUserService userService;
    private final IEventService eventService;
    private final ILogger logger;
    private final ApplicationEventPublisher eventPublisher;

    // return type boolean represents whether the user was already invited or not
    // if false -> invites them
    // if true -> return 400 in Controller to indicate that the user has already
    // been invited, or it is a bad request.
    @Override
    public boolean inviteUser(UUID eventId, UUID userId) {
        EventUsersId compositeId = new EventUsersId(eventId, userId);
        Optional<EventUser> existingEventUser = repository.findById(compositeId);

        if (existingEventUser.isPresent()) {
            // User is already invited
            return existingEventUser.get().getStatus().equals(ParticipationStatus.invited);
        } else {
            // Create a new invitation.
            User user = userService.getUserEntityById(userId);
            Event event = eventService.getEventEntityById(eventId);

            EventUser newEventUser = new EventUser();
            newEventUser.setId(compositeId);
            newEventUser.setEvent(event);
            newEventUser.setUser(user);
            newEventUser.setStatus(ParticipationStatus.invited);

            repository.save(newEventUser);
            return false;
        }
    }

    @Override
    public List<BaseUserDTO> getParticipantsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = repository.findByEvent_IdAndStatus(eventId, ParticipationStatus.participating);

            return eventUsers.stream()
                    .map(eventUser -> UserMapper.toDTO(eventUser.getUser()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving participants for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participants for eventId " + eventId, e);
        }
    }

    @Override
    public List<BaseUserDTO> getInvitedByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = repository.findByEvent_IdAndStatus(eventId, ParticipationStatus.invited);

            return eventUsers.stream()
                    .map(eventUser -> UserMapper.toDTO(eventUser.getUser()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving invited users for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving invited users for eventId " + eventId, e);
        }
    }

    @Override
    public List<UUID> getParticipantUserIdsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = repository.findByEvent_IdAndStatus(eventId, ParticipationStatus.participating);

            return eventUsers.stream()
                    .map(eventUser -> eventUser.getUser().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving participant user IDs for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participant user IDs for eventId " + eventId, e);
        }
    }

    @Override
    public List<UUID> getInvitedUserIdsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = repository.findByEvent_IdAndStatus(eventId, ParticipationStatus.invited);

            return eventUsers.stream()
                    .map(eventUser -> eventUser.getUser().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving invited user IDs for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving invited user IDs for eventId " + eventId, e);
        }
    }

    @Override
    public ParticipationStatus getParticipationStatus(UUID eventId, UUID userId) {
        EventUsersId compositeId = new EventUsersId(eventId, userId);
        return repository.findById(compositeId)
                .map(EventUser::getStatus)
                .orElse(ParticipationStatus.notInvited);
    }

    @Override
    public List<UserDTO> getParticipatingUsersByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = repository.findByEvent_IdAndStatus(eventId, ParticipationStatus.participating);
            return eventUsers.stream()
                    .map(eventUser -> userService.getUserDTOByEntity(eventUser.getUser()))
                    .toList();
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseNotFoundException(EntityType.Event, eventId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    // returns the updated event, with modified participants and invited users
    // invited/participating
    // if true -> change status
    // if false -> return 400 in controller to indicate that the user is not
    // invited/participating
    @Override
    public FullFeedEventDTO toggleParticipation(UUID eventId, UUID userId) {
        EventUser eventUser = repository.findByEvent_IdAndUser_Id(eventId, userId).orElseThrow(() -> new BaseNotFoundException(EntityType.EventUser));

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

        repository.save(eventUser);
        return eventService.getFullEventByEvent(eventService.getEventDTOByEntity(eventUser.getEvent()), userId, new HashSet<>());
    }

    @Override
    public List<EventDTO> getEventsInvitedTo(UUID id) {
        List<EventUser> eventUsers = repository.findByUser_IdAndStatus(id, ParticipationStatus.invited);
        return eventUsers.stream()
                .map(EventUser::getEvent)
                .map(eventService::getEventDTOByEntity)
                .toList();
    }

    private List<UUID> getParticipatingUserIdsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = repository.findEventsByEvent_IdAndStatus(eventId, ParticipationStatus.participating);
            return eventUsers.stream().map((eventUser -> eventUser.getUser().getId())).collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding events by event id: " + e.getMessage());
            throw e;
        }
    }
}
