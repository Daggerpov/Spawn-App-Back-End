package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.EventCreationDTO;
import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FullFeedEventDTO;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.User;

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
                entity.getLocation() != null ? LocationMapper.toDTO(entity.getLocation()).getId() : null, // Map Location to LocationDTO
                entity.getNote(),
                creatorUserId,
                participantUserIds,
                invitedUserIds,
                chatMessageIds
        );
    }

    // Convert DTO to entity
    public static Event toEntity(EventDTO dto, Location location, User creator) {
        return new Event(
                dto.getId(),
                dto.getTitle(),
                dto.getStartTime(),
                dto.getEndTime(),
                location, // Assign the full Location entity
                dto.getNote(),
                creator
        );
    }

    public static List<EventDTO> toDTOList(
            List<Event> entities,
            Map<UUID, UUID> creatorUserIdMap, // Map of Event ID to creatorUserId UserDTO
            Map<UUID, List<UUID>> participantUserIdsMap, // Map of Event ID to participantUserIds
            Map<UUID, List<UUID>> invitedUserIdsMap, // Map of Event ID to invitedUserIds users
            Map<UUID, List<UUID>> chatMessageIdsMap // Map of Event ID to chat messages
    ) {
        return entities.stream()
                .map(entity -> toDTO(
                        entity,
                        creatorUserIdMap.get(entity.getId()), // Fetch the creatorUserId UserDTO
                        participantUserIdsMap.getOrDefault(entity.getId(), List.of()), // Fetch participantUserIds or default empty
                        invitedUserIdsMap.getOrDefault(entity.getId(), List.of()), // Fetch invitedUserIds users or default empty
                        chatMessageIdsMap.getOrDefault(entity.getId(), List.of()) // Fetch chat messages or default empty
                ))
                .collect(Collectors.toList());
    }

    public static List<Event> toEntityList(List<EventDTO> eventDTOS, List<Location> locations, List<User> creators) {
        return eventDTOS.stream()
                .map(dto -> {
                    // Find the Location entity based on the locationId from DTO
                    Location location = locations.stream()
                            .filter(loc -> loc.getId().equals(dto.getLocationId())) // Match LocationDTO's UUID with Location entity
                            .findFirst()
                            .orElse(null);

                    // Find the User entity (creator) based on the creatorUserId from DTO
                    User creator = creators.stream()
                            .filter(user -> user.getId().equals(dto.getCreatorUserId())) // Match creatorUserId with User entity
                            .findFirst()
                            .orElse(null);

                    return toEntity(dto, location, creator); // Convert DTO to entity
                })
                .collect(Collectors.toList());
    }

    public static Event convertFullFeedEventDTOToEventEntity(FullFeedEventDTO dto) {
        Event event = new Event();
        event.setId(dto.getId()); // Set the UUID
        event.setTitle(dto.getTitle()); // Set the title
        event.setStartTime(dto.getStartTime()); // Set the start time
        event.setEndTime(dto.getEndTime()); // Set the end time

        // Convert LocationDTO to Location entity (assuming a similar method exists)
        Location location = LocationMapper.toEntity(dto.getLocation());
        event.setLocation(location); // Set the location

        event.setNote(dto.getNote()); // Set the note

        // Convert FullUserDTO to User entity (assuming a similar method exists)
        User creator = UserMapper.convertFullUserToUserEntity(dto.getCreatorUser());
        event.setCreator(creator); // Set the creator

        return event;
    }

    public static Event fromCreationDTO(EventCreationDTO dto, Location location, User creator) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setStartTime(dto.getStartTime());
        event.setEndTime(dto.getEndTime());
        event.setLocation(location); // Use the saved/persisted location.
        event.setNote(dto.getNote());
        event.setCreator(creator);
        return event;
    }
}
