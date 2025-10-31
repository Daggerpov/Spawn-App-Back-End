package com.danielagapov.spawn.Services.UserStats;

import com.danielagapov.spawn.DTOs.User.Profile.UserStatsDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Repositories.IActivityRepository;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserStatsService implements IUserStatsService {

    private final IActivityRepository activityRepository;
    private final IActivityUserRepository activityUserRepository;
    private final IUserRepository userRepository;

    @Autowired
    public UserStatsService(
            IActivityRepository activityRepository,
            IActivityUserRepository activityUserRepository,
            IUserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.activityUserRepository = activityUserRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "userStatsById", key = "#userId")
    public UserStatsDTO getUserStats(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new BaseNotFoundException(EntityType.User, userId);
        }

        // Get activities created by user
        int spawnsMade = activityRepository.findByCreatorId(userId).size();

        // Get activities participated in (but not created by user)
        List<ActivityUser> participatedActivities = activityUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating);
        
        // Filter out activities created by the user
        int spawnsJoined = (int) participatedActivities.stream()
                .filter(au -> !au.getActivity().getCreator().getId().equals(userId))
                .count();

        // Get all unique users that this user has participated in Activities with
        Set<UUID> peopleMet = new HashSet<>();

        // Add people from activities created by the user
        activityRepository.findByCreatorId(userId).forEach(activity -> {
            activityUserRepository.findByActivity_IdAndStatus(activity.getId(), ParticipationStatus.participating)
                    .forEach(au -> {
                        UUID participantId = au.getUser().getId();
                        if (!participantId.equals(userId)) {
                            peopleMet.add(participantId);
                        }
                    });
        });

        // Add people from activities the user participated in
        participatedActivities.forEach(activityUser -> {
            // Add the creator if it's not the user
            UUID creatorId = activityUser.getActivity().getCreator().getId();
            if (!creatorId.equals(userId)) {
                peopleMet.add(creatorId);
            }

            // Add other participants
            activityUserRepository.findByActivity_IdAndStatus(activityUser.getActivity().getId(), ParticipationStatus.participating)
                    .forEach(au -> {
                        UUID participantId = au.getUser().getId();
                        if (!participantId.equals(userId)) {
                            peopleMet.add(participantId);
                        }
                    });
        });

        return new UserStatsDTO(peopleMet.size(), spawnsMade, spawnsJoined);
    }
} 