package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.User.AuthResponseDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Models.User.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserMapper {

    public static BaseUserDTO toDTO(User user) {
        return new BaseUserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString(),
                user.getHasCompletedOnboarding()
        );
    }

    public static AuthResponseDTO toAuthResponseDTO(User user) {
        BaseUserDTO baseUserDTO = toDTO(user);
        return new AuthResponseDTO(baseUserDTO, user.getStatus());
    }

    public static AuthResponseDTO toAuthResponseDTO(User user, boolean isOAuthUser) {
        BaseUserDTO baseUserDTO = toDTO(user);
        return new AuthResponseDTO(baseUserDTO, user.getStatus(), isOAuthUser);
    }

    public static List<BaseUserDTO> toDTOList(List<User> users) {
        return users.stream().map(UserMapper::toDTO).toList();
    }

    public static UserDTO toDTO(User user, List<UUID> friendUserIds) {

        return new UserDTO(
                user.getId(),
                friendUserIds,
                user.getUsername(),
                user.getProfilePictureUrlString(),
                user.getName(),
                user.getBio(),
                user.getEmail(),
                user.getHasCompletedOnboarding()
        );
    }

    /**
     * WARNING: This method creates a User entity with incomplete data (missing phoneNumber, status, etc.).
     * It should ONLY be used for creating new users, never for updating existing ones.
     * Use userService.getUserEntityById() to get complete User entities for updates.
     */
    public static User toEntity(BaseUserDTO dto) {
        return new User(
                dto.getId(),
                dto.getUsername(),
                dto.getProfilePicture(),
                dto.getName(),
                dto.getBio(),
                dto.getEmail()
        );
    }

    public static List<UserDTO> toDTOList(List<User> users, Map<User, List<UUID>> friendUserIdsMap) {
        return users.stream()
                .map(user -> toDTO(
                        user,
                        friendUserIdsMap.getOrDefault(user, List.of())
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
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePicture(),
                user.getHasCompletedOnboarding()
        );
    }

    public static List<BaseUserDTO> toBaseDTOList(List<UserDTO> userDTOs) {
        return userDTOs.stream()
                .map(UserMapper::toBaseDTO)
                .collect(Collectors.toList());
    }

    public static UserDTO toDTOFromCreationUserDTO(UserCreationDTO userCreationDTO) {
        return new UserDTO(
                userCreationDTO.getId(),
                null,
                userCreationDTO.getUsername(),
                null,
                userCreationDTO.getName(),
                userCreationDTO.getBio(),
                userCreationDTO.getEmail(),
                false // Default value for new users
        );
    }

}
