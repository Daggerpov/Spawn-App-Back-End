package com.danielagapov.spawn.chat.internal.repositories;

import com.danielagapov.spawn.chat.internal.domain.ChatMessage;
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

    @Query("SELECT cm.activity.id, cm.id FROM ChatMessage cm WHERE cm.activity.id IN :activityIds ORDER BY cm.activity.id, cm.timestamp DESC")
    List<Object[]> findChatMessageIdsByActivityIds(@Param("activityIds") List<UUID> activityIds);
}
