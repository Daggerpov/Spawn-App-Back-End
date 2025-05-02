package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.DTOs.UserStatsDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserStatsService {

    private final IEventRepository eventRepository;
    private final IEventUserRepository eventUserRepository;
    private final IUserRepository userRepository;

    @Autowired
    public UserStatsService(
            IEventRepository eventRepository,
            IEventUserRepository eventUserRepository,
            IUserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.eventUserRepository = eventUserRepository;
        this.userRepository = userRepository;
    }

    public UserStatsDTO getUserStats(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        // Get events created by user
        int spawnsMade = eventRepository.findByCreatorId(userId).size();

        // Get events participated in (but not created by user)
        List<EventUser> participatedEvents = eventUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating);
        
        // Filter out events created by the user
        int spawnsJoined = (int) participatedEvents.stream()
                .filter(eu -> !eu.getEvent().getCreator().getId().equals(userId))
                .count();

        // Get all unique users that this user has participated in events with
        Set<UUID> peopleMet = new HashSet<>();

        // Add people from events created by the user
        eventRepository.findByCreatorId(userId).forEach(event -> {
            eventUserRepository.findByEvent_IdAndStatus(event.getId(), ParticipationStatus.participating)
                    .forEach(eu -> {
                        UUID participantId = eu.getUser().getId();
                        if (!participantId.equals(userId)) {
                            peopleMet.add(participantId);
                        }
                    });
        });

        // Add people from events the user participated in
        participatedEvents.forEach(eventUser -> {
            // Add the creator if it's not the user
            UUID creatorId = eventUser.getEvent().getCreator().getId();
            if (!creatorId.equals(userId)) {
                peopleMet.add(creatorId);
            }

            // Add other participants
            eventUserRepository.findByEvent_IdAndStatus(eventUser.getEvent().getId(), ParticipationStatus.participating)
                    .forEach(eu -> {
                        UUID participantId = eu.getUser().getId();
                        if (!participantId.equals(userId)) {
                            peopleMet.add(participantId);
                        }
                    });
        });

        return new UserStatsDTO(peopleMet.size(), spawnsMade, spawnsJoined);
    }
} 