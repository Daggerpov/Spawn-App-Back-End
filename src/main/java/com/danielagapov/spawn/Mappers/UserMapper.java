package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.DTOMappingException;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    private static IUserFriendTagRepository uft_repository;
    private static IUserRepository user_repository;
    private static IFriendTagRepository ft_repository;

    public static UserDTO toDTO(User user) {

        return new UserDTO(
                user.getId(),
                uft_repository.findFriendsByTagId(user.getFriends())
                        .stream()
                        .map(uuid -> toDTO(user_repository.findById(uuid).orElseThrow(() ->
                                new DTOMappingException("failed to find friends associated to user."))))
                        .collect(Collectors.toList()),
                user.getProfilePicture(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                ft_repository.findTagsByOwner(user.getId())
                        .stream()
                        .map(uuid -> FriendTagMapper.toDTO(ft_repository.findById(uuid).orElseThrow(() ->
                                new DTOMappingException("failed to map friend tag id to object"))))
                        .collect(Collectors.toList()),
                user.getEmail()
        );
    }

    public static User toEntity(UserDTO dto) {
        // TODO: convert DTO list back into tag list/SQL relations
        User user = new User();
        user.setId(dto.id());
        user.setUsername(dto.username());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setBio(dto.bio());
        user.setProfilePicture(dto.profilePicture());
        return user;
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
