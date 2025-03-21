package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserMapper {

    public static BaseUserDTO toDTO(User user) {
        return new BaseUserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString()
        );
    }

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

    public static User toEntity(BaseUserDTO dto) {
        return new User(
                dto.getId(),
                dto.getUsername(),
                dto.getProfilePicture(),
                dto.getFirstName(),
                dto.getLastName(),
                dto.getBio(),
                dto.getEmail()
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

    public static List<User> toEntityList(List<BaseUserDTO> userDTOs) {
        return userDTOs.stream()
                .map(UserMapper::toEntity)
                .collect(Collectors.toList());
    }

    public static BaseUserDTO toBaseDTO(UserDTO user) {
        return new BaseUserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePicture()
        );
    }

}
