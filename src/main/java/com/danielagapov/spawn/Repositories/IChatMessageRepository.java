package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.ChatMessage.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IChatMessageRepository extends JpaRepository<ChatMessage, Long> { }
