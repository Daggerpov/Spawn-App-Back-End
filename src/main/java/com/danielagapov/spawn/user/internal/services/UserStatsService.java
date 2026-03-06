package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.Profile.UserStatsDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.feign.ActivityServiceClient;
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

    private final ActivityServiceClient activityServiceClient;
    private final IUserRepository userRepository;

    @Autowired
    public UserStatsService(
            ActivityServiceClient activityServiceClient,
            IUserRepository userRepository) {
        this.activityServiceClient = activityServiceClient;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "userStatsById", key = "#userId")
    public UserStatsDTO getUserStats(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new BaseNotFoundException(EntityType.User, userId);
        }

        List<UUID> createdActivityIds = activityServiceClient.getActivityIdsCreatedByUser(userId);
        int spawnsMade = createdActivityIds.size();

        List<UUID> participatedActivityIds = activityServiceClient.getActivityIdsByUserAndStatus(userId, ParticipationStatus.participating);
        
        Set<UUID> createdSet = new HashSet<>(createdActivityIds);
        int spawnsJoined = (int) participatedActivityIds.stream()
                .filter(activityId -> !createdSet.contains(activityId))
                .count();

        Set<UUID> peopleMet = new HashSet<>();

        for (UUID activityId : createdActivityIds) {
            List<UUID> participantIds = activityServiceClient.getParticipantUserIds(activityId, ParticipationStatus.participating);
            for (UUID participantId : participantIds) {
                if (!participantId.equals(userId)) {
                    peopleMet.add(participantId);
                }
            }
        }

        for (UUID activityId : participatedActivityIds) {
            UUID creatorId = activityServiceClient.getCreatorId(activityId);
            if (creatorId != null && !creatorId.equals(userId)) {
                peopleMet.add(creatorId);
            }

            List<UUID> participantIds = activityServiceClient.getParticipantUserIds(activityId, ParticipationStatus.participating);
            for (UUID participantId : participantIds) {
                if (!participantId.equals(userId)) {
                    peopleMet.add(participantId);
                }
            }
        }

        return new UserStatsDTO(peopleMet.size(), spawnsMade, spawnsJoined);
    }
} 