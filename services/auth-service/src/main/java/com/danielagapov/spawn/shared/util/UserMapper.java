package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.user.api.dto.AuthResponseDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserCreationDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Auth-service version of UserMapper.
 * Contains only the mapping methods needed by the auth service.
 */
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
                null
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

    public static UserDTO toDTOFromCreationUserDTO(UserCreationDTO userCreationDTO) {
        return new UserDTO(
                userCreationDTO.getId(),
                null,
                userCreationDTO.getUsername(),
                null,
                userCreationDTO.getName(),
                userCreationDTO.getBio(),
                userCreationDTO.getEmail(),
                false
        );
    }
}
