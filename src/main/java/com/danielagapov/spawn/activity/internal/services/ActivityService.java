package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.activity.api.IActivityService;
import com.danielagapov.spawn.activity.api.dto.UserIdActivityTimeDTO;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.domain.ActivityUser;
import com.danielagapov.spawn.activity.internal.repositories.IActivityRepository;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the Activity module's public API.
 * 
 * This service provides read-only access to activity participation data
 * for other modules (User, Chat, etc.) without exposing internal repositories.
 * 
 * Part of Phase 3: Shared Data Resolution in Spring Modulith refactoring.
 */
@Service
public class ActivityService implements IActivityService {
    
    private final IActivityUserRepository activityUserRepository;
    private final IActivityRepository activityRepository;
    
    public ActivityService(IActivityUserRepository activityUserRepository,
                           IActivityRepository activityRepository) {
        this.activityUserRepository = activityUserRepository;
        this.activityRepository = activityRepository;
    }
    
    // ==================== Participant Queries ====================
    
    @Override
    public List<UUID> getParticipantUserIdsByActivityIdAndStatus(UUID activityId, ParticipationStatus status) {
        return activityUserRepository.findByActivity_IdAndStatus(activityId, status)
                .stream()
                .map(au -> au.getUser().getId())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UUID> getActivityIdsByUserIdAndStatus(UUID userId, ParticipationStatus status) {
        return activityUserRepository.findByUser_IdAndStatus(userId, status)
                .stream()
                .map(au -> au.getActivity().getId())
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isUserParticipantWithStatus(UUID activityId, UUID userId, ParticipationStatus status) {
        return activityUserRepository.findByActivity_IdAndUser_Id(activityId, userId)
                .map(au -> au.getStatus() == status)
                .orElse(false);
    }
    
    @Override
    public int getParticipantCountByStatus(UUID activityId, ParticipationStatus status) {
        return activityUserRepository.findByActivity_IdAndStatus(activityId, status).size();
    }
    
    // ==================== Activity History Queries ====================
    
    @Override
    public List<UUID> getPastActivityIdsForUser(UUID userId, ParticipationStatus status, OffsetDateTime now, Limit limit) {
        return activityUserRepository.findPastActivityIdsForUser(userId, status, now, limit);
    }
    
    @Override
    public List<UserIdActivityTimeDTO> getOtherUserIdsByActivityIds(List<UUID> activityIds, UUID excludeUserId, ParticipationStatus status) {
        return activityUserRepository.findOtherUserIdsByActivityIds(activityIds, excludeUserId, status);
    }
    
    // ==================== Shared Activities Queries ====================
    
    @Override
    public int getSharedActivitiesCount(UUID userId1, UUID userId2, ParticipationStatus status) {
        // Get all activities where user1 has participated
        List<ActivityUser> user1Activities = activityUserRepository.findByUser_IdAndStatus(userId1, status);
        
        if (user1Activities.isEmpty()) {
            return 0;
        }
        
        // Extract activity IDs from user1's participated activities
        Set<UUID> user1ActivityIds = user1Activities.stream()
                .map(au -> au.getActivity().getId())
                .collect(Collectors.toSet());
        
        // Get all activities where user2 has participated
        List<ActivityUser> user2Activities = activityUserRepository.findByUser_IdAndStatus(userId2, status);
        
        // Count how many activities overlap between the two users
        return (int) user2Activities.stream()
                .map(au -> au.getActivity().getId())
                .filter(user1ActivityIds::contains)
                .count();
    }
    
    // ==================== Activity Creator Queries ====================
    
    @Override
    public UUID getActivityCreatorId(UUID activityId) {
        return activityRepository.findById(activityId)
                .map(activity -> activity.getCreator().getId())
                .orElse(null);
    }
    
    @Override
    public List<UUID> getActivityIdsCreatedByUser(UUID userId) {
        return activityRepository.findByCreatorId(userId)
                .stream()
                .map(Activity::getId)
                .collect(Collectors.toList());
    }
}

