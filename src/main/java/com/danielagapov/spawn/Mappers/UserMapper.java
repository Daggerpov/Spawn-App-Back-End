package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserDTO toDTO(User user, List<UUID> friendUserIds, List<UUID> friendTagIds) {

        return new UserDTO(
                user.getId(),
                friendUserIds,
                user.getUsername(),
                user.getProfilePictureUrlString(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                friendTagIds,
                user.getEmail()
        );
    }

    public static User toEntity(UserDTO dto) {
        return new User(
                dto.id(),
                dto.username(),
                dto.profilePicture(),
                dto.firstName(),
                dto.lastName(),
                dto.bio(),
                dto.email()
        );
    }

    public static List<UserDTO> toDTOList(List<User> users, Map<User, List<UUID>> friendUserIdsMap, Map<User, List<UUID>> friendTagIdsMap) {
        return users.stream()
                .map(user -> toDTO(
                        user,
                        friendUserIdsMap.getOrDefault(user, List.of()),
                        friendTagIdsMap.getOrDefault(user, List.of())
                ))
                .collect(Collectors.toList());
    }

    public static List<User> toEntityList(List<UserDTO> userDTOs) {
        return userDTOs.stream()
                .map(UserMapper::toEntity)
                .collect(Collectors.toList());
    }

    public static User toEntityFromFullDTO(FullUserDTO dto) {
        return new User(
                dto.id(),
                dto.username(),
                dto.profilePicture(),
                dto.firstName(),
                dto.lastName(),
                dto.bio(),
                dto.email()
        );
    }
}
