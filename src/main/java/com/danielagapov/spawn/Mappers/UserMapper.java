package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getProfilePicture(),
                // TODO: supply real value once relations are setup within entities:
                null
        );
    }

    public static User toEntity(UserDTO dto) {
        // TODO: enable this once relations are setup within entities:

        return new User(
                dto.id(),
                dto.username(),
                dto.firstName(),
                dto.lastName(),
                dto.bio(),
                dto.profilePicture()
        );
        // TODO: setup later once relations are setup within entities

    }

    public static List<UserDTO> toDTOList(List<User> users) {
        return users.stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<User> toEntityList(List<UserDTO> userDTOs) {
        return userDTOs.stream()
                .map(UserMapper::toEntity)
                .collect(Collectors.toList());
    }
}
