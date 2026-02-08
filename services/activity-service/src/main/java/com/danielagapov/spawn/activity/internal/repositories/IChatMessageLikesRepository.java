package com.danielagapov.spawn.activity.internal.repositories;

import com.danielagapov.spawn.activity.internal.domain.ChatMessage;
import com.danielagapov.spawn.activity.internal.domain.ChatMessageLikes;
import com.danielagapov.spawn.activity.internal.domain.ChatMessageLikesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IChatMessageLikesRepository extends JpaRepository<ChatMessageLikes, ChatMessageLikesId> {
    boolean existsByChatMessage_IdAndUser_Id(UUID chatMessageId, UUID userId);
    void deleteByChatMessage_IdAndUser_Id(UUID chatMessageId, UUID userId);
    List<ChatMessageLikes> findByChatMessage(ChatMessage chatMessage);
}
