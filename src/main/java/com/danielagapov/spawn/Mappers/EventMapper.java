package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;

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
                entity.getLocation() != null ? LocationMapper.toDTO(entity.getLocation()) : null, // Map Location to LocationDTO
                entity.getNote(),
                null, // Placeholder for creator
                null, // Placeholder for participants
                null, // Placeholder for invited
                null  // Placeholder for chatMessages
        );
    }

    // Convert DTO to entity
    public static Event toEntity(EventDTO dto, Location location) {
        Event event = new Event();
        event.setId(dto.id());
        event.setTitle(dto.title());
        event.setStartTime(dto.startTime());
        event.setEndTime(dto.endTime());
        event.setLocation(location); // Assign the full Location entity
        event.setNote(dto.note());
        return event;
    }

    public static List<EventDTO> toDTOList(List<Event> entities) {
        return entities.stream()
                .map(EventMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<Event> toEntityList(List<EventDTO> eventDTOS, List<Location> locations) {
        return eventDTOS.stream()
                .map(dto -> {
                    Location location = locations.stream()
                            .filter(loc -> loc.getId().equals(dto.location().id())) // Match LocationDTO's UUID with Location entity
                            .findFirst()
                            .orElse(null);
                    return toEntity(dto, location);
                })
                .collect(Collectors.toList());
    }
}
