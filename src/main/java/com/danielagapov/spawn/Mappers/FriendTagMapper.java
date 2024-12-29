package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Exceptions.Base.DTOMappingException;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import com.danielagapov.spawn.Models.User;

public class FriendTagMapper {
    public static FriendTagDTO toDTO(FriendTag entity, IUserFriendTagRepository uft_repository, IUserRepository user_repository) {
        /*List<UserDTO> friends = uft_repository.findFriendsByTagId(entity.getId())
                .stream()
                .map(uuid -> UserMapper.toDTO(user_repository.findById(uuid).orElseThrow(() ->
                    new DTOMappingException("Failed to map friend tag to friends"))))
                .collect(Collectors.toList());
        UserDTO owner = UserMapper.toDTO(user_repository.findById(entity.getOwner()).orElseThrow(() ->
                new DTOMappingException("Failed to map owner to from friend tag")));*/
        return new FriendTagDTO(
                entity.getId(),
                entity.getDisplayName(),
                entity.getColorHexCode(),
                entity.getOwner().getId(),
                entity.getFriends().stream().map(User::getId).collect(Collectors.toList())
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto, IUserRepository userRepository) {
        FriendTag friendTag = new FriendTag();
        friendTag.setId(dto.id());
        friendTag.setDisplayName(dto.displayName());
        friendTag.setColorHexCode(dto.colorHexCode());
        friendTag.setOwner(userRepository.findById(dto.ownerId()).orElseThrow(() ->
                new DTOMappingException("failed to map owner ID to a user in the database: " + dto.ownerId())));
        friendTag.setFriends(dto.friends().stream()
                .map(friendId -> userRepository.findById(friendId).orElseThrow(() ->
                        new DTOMappingException("failed to map friend ID to user in the database")))
                .collect(Collectors.toList()));
        return friendTag;
    }

    public static List<FriendTagDTO> toDTOList(List<FriendTag> entities, IUserFriendTagRepository uftRepository, IUserRepository userRepository) {
        return entities.stream()
                .map(friendTag -> toDTO(friendTag, uftRepository, userRepository))
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> dtos, IUserRepository userRepository) {
        return dtos.stream()
                .map(friendTag -> toEntity(friendTag, userRepository))
                .collect(Collectors.toList());
    }
}
