package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.Profile.UserStatsDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.activity.api.IActivityService;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserStatsService implements IUserStatsService {

    private final IActivityService activityService;
    private final IUserRepository userRepository;

    @Autowired
    public UserStatsService(
            IActivityService activityService,
            IUserRepository userRepository) {
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "userStatsById", key = "#userId")
    public UserStatsDTO getUserStats(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new BaseNotFoundException(EntityType.User, userId);
        }

        // Get activities created by user
        List<UUID> createdActivityIds = activityService.getActivityIdsCreatedByUser(userId);
        int spawnsMade = createdActivityIds.size();

        // Get activities participated in
        List<UUID> participatedActivityIds = activityService.getActivityIdsByUserIdAndStatus(userId, ParticipationStatus.participating);
        
        // Filter out activities created by the user (spawns joined = participated but not created)
        Set<UUID> createdSet = new HashSet<>(createdActivityIds);
        int spawnsJoined = (int) participatedActivityIds.stream()
                .filter(activityId -> !createdSet.contains(activityId))
                .count();

        // Get all unique users that this user has participated in Activities with
        Set<UUID> peopleMet = new HashSet<>();

        // Add people from activities created by the user
        for (UUID activityId : createdActivityIds) {
            List<UUID> participantIds = activityService.getParticipantUserIdsByActivityIdAndStatus(activityId, ParticipationStatus.participating);
            for (UUID participantId : participantIds) {
                if (!participantId.equals(userId)) {
                    peopleMet.add(participantId);
                }
            }
        }

        // Add people from activities the user participated in
        for (UUID activityId : participatedActivityIds) {
            // Add the creator if it's not the user
            UUID creatorId = activityService.getActivityCreatorId(activityId);
            if (creatorId != null && !creatorId.equals(userId)) {
                peopleMet.add(creatorId);
            }

            // Add other participants
            List<UUID> participantIds = activityService.getParticipantUserIdsByActivityIdAndStatus(activityId, ParticipationStatus.participating);
            for (UUID participantId : participantIds) {
                if (!participantId.equals(userId)) {
                    peopleMet.add(participantId);
                }
            }
        }

        return new UserStatsDTO(peopleMet.size(), spawnsMade, spawnsJoined);
    }
} 