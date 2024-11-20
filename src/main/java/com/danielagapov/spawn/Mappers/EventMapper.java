package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Models.Event.Event;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.ChatMessage.ChatMessage;

import java.util.List;
import java.util.stream.Collectors;

public class EventMapper {

    // Convert entity to DTO
    public static EventDTO toDTO(Event entity) {
        return new EventDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getLocation(),
                entity.getNote(),

                // TODO: replace these with proper DTOs, once entities are configured
                // to allow for joined tables and proper relationships
                null,
                null,
                null,
                null

                /*
                UserMapper.toDTO(entity.getCreator()), // Map creator to UserDTO
                UserMapper.toDTOList(entity.getParticipants()),
                UserMapper.toDTOList(entity.getInvited()),
                ChatMessageMapper.toDTOList(entity.getChatMessages())
                */
        );
    }

    // Convert DTO to entity
    public static Event toEntity(EventDTO dto) {
        Event event = new Event();
        event.setId(dto.id());
        event.setTitle(dto.title());
        event.setStartTime(dto.startTime());
        event.setEndTime(dto.endTime());
        event.setLocation(dto.location());
        event.setNote(dto.note());

        // TODO: replace these with proper DTOs, once entities are configured
        // to allow for joined tables and proper relationships
        /*
        event.setCreator(UserMapper.toEntity(dto.creator()));
        event.setParticipants(UserMapper.toEntityList(dto.participants()));
        event.setInvited(UserMapper.toEntityList(dto.invited()));
        event.setChatMessages(ChatMessageMapper.toEntityList(dto.chatMessages()));
        */

        return event;
    }

    public static List<EventDTO> toDTOList(List<Event> entities) {
        return entities.stream()
                .map(EventMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<Event> toEntityList(List<EventDTO> dtos) {
        return dtos.stream()
                .map(EventMapper::toEntity)
                .collect(Collectors.toList());
    }
}
