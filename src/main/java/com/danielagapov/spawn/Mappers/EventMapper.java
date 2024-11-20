package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.Models.Event.Event;

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
