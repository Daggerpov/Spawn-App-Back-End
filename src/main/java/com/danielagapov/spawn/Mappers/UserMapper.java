package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.FriendTag.FriendTagService;
import com.danielagapov.spawn.Services.User.UserService;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserDTO toDTO(User user, UserService userService, FriendTagService ftService) {

        return new UserDTO(
                user.getId(),
                userService.getUserFriends(user.getId()),
                user.getUsername(),
                user.getProfilePicture(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                ftService.getFriendTagsByOwnerId(user.getId()),
                user.getEmail()
        );
    }

    public static User toEntity(UserDTO dto) {
        return new User(
                dto.id(),
                dto.friendTags() != null ? dto.friendTags().get(0).id() : null, // First tag is always "everyone" tag (should improve later)
                dto.username(),
                dto.profilePicture(),
                dto.firstName(),
                dto.lastName(),
                dto.bio(),
                dto.email()
        );
    }

    public static List<UserDTO> toDTOList(List<User> users, UserService userService, FriendTagService ftService) {
        return users.stream()
                .map(user -> toDTO(user, userService, ftService))
                .collect(Collectors.toList());
    }

    public static List<User> toEntityList(List<UserDTO> userDTOs) {
        return userDTOs.stream()
                .map(UserMapper::toEntity)
                .collect(Collectors.toList());
    }
}
