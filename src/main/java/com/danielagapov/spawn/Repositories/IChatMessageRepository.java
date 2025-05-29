package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> getChatMessagesByActivityId(UUID ActivityId);

    List<ChatMessage> getChatMessagesByActivityIdOrderByTimestampDesc(UUID ActivityId);
}
