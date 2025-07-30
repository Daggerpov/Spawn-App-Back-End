package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.ChatMessageLikes;
import com.danielagapov.spawn.Models.CompositeKeys.ChatMessageLikesId;
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
