package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserDTO toDTO(User user, List<UserDTO> friends, List<FriendTagDTO> friendTags) {

        return new UserDTO(
                user.getId(),
                friends,
                user.getUsername(),
                user.getProfilePicture(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                friendTags,
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
                dto.email(),
                dto.friendTags().get(0).id()
        );
    }

    public static List<UserDTO> toDTOList(List<User> users, Map<User, List<UserDTO>> friendsMap, Map<User, List<FriendTagDTO>> friendTagsMap) {
        return users.stream()
                .map(user -> toDTO(
                        user,
                        friendsMap.getOrDefault(user, List.of()),
                        friendTagsMap.getOrDefault(user, List.of())
                ))
                .collect(Collectors.toList());
    }

    public static List<User> toEntityList(List<UserDTO> userDTOs) {
        return userDTOs.stream()
                .map(UserMapper::toEntity)
                .collect(Collectors.toList());
    }
}
