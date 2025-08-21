package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.DTOs.UserIdActivityTimeDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.CompositeKeys.ActivityUsersId;
import com.danielagapov.spawn.Models.ActivityUser;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IActivityUserRepository extends JpaRepository<ActivityUser, ActivityUsersId> {
    List<ActivityUser> findByActivity_Id(UUID activityId);

    List<ActivityUser> findByUser_Id(UUID userId);

    // Find activity users by activity ID and participation status
    List<ActivityUser> findByActivity_IdAndStatus(UUID activityId, ParticipationStatus status);

    // Find activity users by user ID and participation status
    List<ActivityUser> findByUser_IdAndStatus(UUID userId, ParticipationStatus status);

    List<ActivityUser> findActivitiesByActivity_IdAndStatus(UUID activityId, ParticipationStatus status);

    @Query("SELECT au.activity.id FROM ActivityUser au WHERE au.user.id = :userId AND au.status = :status AND au.activity.endTime <= :now")
    List<UUID> findPastActivityIdsForUser(@Param("userId") UUID userId, @Param("status") ParticipationStatus status, @Param("now") OffsetDateTime now, Limit limit);

    @Query("SELECT DISTINCT new com.danielagapov.spawn.DTOs.UserIdActivityTimeDTO(au.user.id, MAX(au.activity.startTime)) FROM ActivityUser au WHERE au.activity.id IN :activityIds AND au.status = :status AND au.user.id != :userId GROUP BY au.user.id ORDER BY MAX(au.activity.startTime) DESC")
    List<UserIdActivityTimeDTO> findOtherUserIdsByActivityIds(List<UUID> activityIds, UUID userId, ParticipationStatus status);

    Optional<ActivityUser> findByActivity_IdAndUser_Id(UUID activityId, UUID userId);
    
    /**
     * Batch query to get participant user IDs for multiple activities at once.
     * This prevents N+1 query problems when loading multiple activities.
     * 
     * @param activityIds List of activity IDs to get participants for
     * @param status The participation status to filter by
     * @return Map of activity ID to list of user IDs
     */
    @Query("SELECT au.activity.id, au.user.id FROM ActivityUser au WHERE au.activity.id IN :activityIds AND au.status = :status")
    List<Object[]> findUserIdsByActivityIdsAndStatus(@Param("activityIds") List<UUID> activityIds, @Param("status") ParticipationStatus status);
    
    /**
     * Batch query to get all activity user relationships for multiple activities.
     * More efficient than separate queries for each status.
     * 
     * @param activityIds List of activity IDs
     * @return List of ActivityUser objects for all requested activities
     */
    @Query("SELECT au FROM ActivityUser au WHERE au.activity.id IN :activityIds")
    List<ActivityUser> findAllByActivityIds(@Param("activityIds") List<UUID> activityIds);

    @Query("SELECT au FROM ActivityUser au JOIN au.activity a WHERE au.user.id = :userId AND au.status = :status ORDER BY a.lastUpdated DESC LIMIT 1")
    Optional<ActivityUser> findTopByUserIdAndStatusOrderByActivityLastUpdatedDesc(@Param("userId") UUID userId, @Param("status") ParticipationStatus status);
}
