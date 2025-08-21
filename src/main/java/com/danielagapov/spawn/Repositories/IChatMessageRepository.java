package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.activity.id = :activityId ORDER BY cm.timestamp DESC")
    List<ChatMessage> getChatMessagesByActivityIdOrderByTimestampDesc(@Param("activityId") UUID activityId);
    
    /**
     * Batch query to get chat message IDs for multiple activities at once.
     * This prevents N+1 query problems when loading multiple activities.
     * 
     * @param activityIds List of activity IDs to get chat messages for
     * @return Map data as Object[] with activity ID and chat message ID
     */
    @Query("SELECT cm.activity.id, cm.id FROM ChatMessage cm WHERE cm.activity.id IN :activityIds ORDER BY cm.activity.id, cm.timestamp DESC")
    List<Object[]> findChatMessageIdsByActivityIds(@Param("activityIds") List<UUID> activityIds);
    
    /**
     * Batch query to get all chat messages for multiple activities.
     * 
     * @param activityIds List of activity IDs
     * @return List of ChatMessage objects for all requested activities
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.activity.id IN :activityIds ORDER BY cm.activity.id, cm.timestamp DESC")
    List<ChatMessage> findAllByActivityIds(@Param("activityIds") List<UUID> activityIds);
}
