package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.user.api.dto.AuthResponseDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.MinimalFriendDTO;
import com.danielagapov.spawn.user.api.dto.UserCreationDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class UserMapper {

    public static BaseUserDTO toDTO(User user) {
        return new BaseUserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString(),
                user.getHasCompletedOnboarding(),
                null  // provider not specified
        );
    }

    public static BaseUserDTO toDTOWithProvider(User user, String provider) {
        return new BaseUserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString(),
                user.getHasCompletedOnboarding(),
                provider
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

    public static AuthResponseDTO toAuthResponseDTO(User user, boolean isOAuthUser, String provider) {
        BaseUserDTO baseUserDTO = toDTOWithProvider(user, provider);
        return new AuthResponseDTO(baseUserDTO, user.getStatus(), isOAuthUser);
    }

    public static List<BaseUserDTO> toDTOList(List<User> users) {
        return users.stream().map(UserMapper::toDTO).toList();
    }

    /**
     * Convert User entity to MinimalFriendDTO with only essential fields.
     * This reduces memory usage when displaying friends in selection lists.
     */
    public static MinimalFriendDTO toMinimalFriendDTO(User user) {
        return new MinimalFriendDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getProfilePictureUrlString()
        );
    }

    /**
     * Convert list of User entities to MinimalFriendDTO list.
     */
    public static List<MinimalFriendDTO> toMinimalFriendDTOList(List<User> users) {
        return users.stream().map(UserMapper::toMinimalFriendDTO).toList();
    }

    /**
     * Convert MinimalFriendDTO to User entity (for conversion operations).
     * WARNING: This creates an incomplete User entity - only id, username, name, profilePicture are set.
     */
    public static User toEntity(MinimalFriendDTO dto) {
        return new User(
                dto.getId(),
                dto.getUsername(),
                dto.getProfilePicture(),
                dto.getName(),
                null,  // bio not available in MinimalFriendDTO
                null   // email not available in MinimalFriendDTO
        );
    }

    /**
     * Convert list of MinimalFriendDTO to list of User entities.
     */
    public static List<User> toEntityList(List<MinimalFriendDTO> dtos) {
        return dtos.stream()
                .map(UserMapper::toEntity)
                .collect(Collectors.toList());
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
