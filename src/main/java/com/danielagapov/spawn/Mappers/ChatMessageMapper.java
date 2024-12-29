package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;

import java.util.List;
import java.util.stream.Collectors;

public class ChatMessageMapper {
    // by far the simplest mapping, since it's essentially 1-to-1

    public static ChatMessageDTO toDTO(ChatMessage entity, IUserFriendTagRepository uftRepository,
                                       IUserRepository userRepository) {
        return new ChatMessageDTO(
                entity.getId(),
                entity.getContent(),
                entity.getTimestamp(),
                UserMapper.toDTO(entity.getUserSender(), uftRepository, userRepository),
                entity.getEvent().getId(),
                UserMapper.toDTOList(entity.getLikedBy(), uftRepository, userRepository)
        );
    }

    public static ChatMessage toEntity(ChatMessageDTO dto, User userSender,
                                       Event event, IUserRepository userRepository) {
        return new ChatMessage(
                dto.id(),
                dto.content(),
                dto.timestamp(),
                userSender,
                event,
                UserMapper.toEntityList(dto.likedBy(), userRepository)
        );
    }

    public static List<ChatMessageDTO> toDTOList(List<ChatMessage> chatMessages, IUserFriendTagRepository uftRepository,
                                                 IUserRepository userRepository) {
        return chatMessages.stream()
                .map(chatMessage -> toDTO(chatMessage, uftRepository, userRepository))
                .collect(Collectors.toList());
    }

    public static List<ChatMessage> toEntityList(List<ChatMessageDTO> chatMessageDTOs, List<User> users,
                                                 List<Event> events, IUserRepository userRepository) {
        return chatMessageDTOs.stream()
                .map(dto -> {
                    User userSender = users.stream()
                            .filter(user -> user.getId().equals(dto.userSender().id()))
                            .findFirst()
                            .orElse(null);
                    Event event = events.stream()
                            .filter(ev -> ev.getId().equals(dto.eventId()))
                            .findFirst()
                            .orElse(null);
                    return toEntity(dto, userSender, event, userRepository);
                })
                .collect(Collectors.toList());
    }
}