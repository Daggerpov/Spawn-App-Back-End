package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventMapper {

    // Convert entity to DTO
    public static EventDTO toDTO(Event entity, UUID creatorUserId, List<UUID> participantUserIds, List<UUID> invitedUserIds, List<UUID> chatMessageIds) {
        return new EventDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getLocation() != null ? LocationMapper.toDTO(entity.getLocation()).id() : null, // Map Location to LocationDTO
                entity.getNote(),
                creatorUserId,
                participantUserIds,
                invitedUserIds,
                chatMessageIds
        );
    }

    // Convert DTO to entity
    public static Event toEntity(EventDTO dto, Location location) {
        return new Event(
                dto.id(),
                dto.title(),
                dto.startTime(),
                dto.endTime(),
                location, // Assign the full Location entity
                dto.note(),
                UserMapper.toEntity(dto.creatorUserId())
        );
    }

    public static List<EventDTO> toDTOList(
            List<Event> entities,
            Map<UUID, UserDTO> creatorMap, // Map of Event ID to creatorUserId UserDTO
            Map<UUID, List<UserDTO>> participantsMap, // Map of Event ID to participantUserIds
            Map<UUID, List<UserDTO>> invitedMap, // Map of Event ID to invitedUserIds users
            Map<UUID, List<ChatMessageDTO>> chatMessagesMap // Map of Event ID to chat messages
    ) {
        return entities.stream()
                .map(entity -> toDTO(
                        entity,
                        creatorMap.get(entity.getId()), // Fetch the creatorUserId UserDTO
                        participantsMap.getOrDefault(entity.getId(), List.of()), // Fetch participantUserIds or default empty
                        invitedMap.getOrDefault(entity.getId(), List.of()), // Fetch invitedUserIds users or default empty
                        chatMessagesMap.getOrDefault(entity.getId(), List.of()) // Fetch chat messages or default empty
                ))
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
