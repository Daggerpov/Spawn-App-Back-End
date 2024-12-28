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

public class FriendTagMapper {
    private static IUserRepository user_repository;
    private static IUserFriendTagRepository uft_repository;

    public static FriendTagDTO toDTO(FriendTag entity) {
        List<UserDTO> friends = uft_repository.findFriendsByTagId(entity.getId())
                .stream()
                .map(uuid -> UserMapper.toDTO(user_repository.findById(uuid).orElseThrow(() ->
                    new DTOMappingException("Failed to map friend tag to friends"))))
                .collect(Collectors.toList());
        UserDTO owner = UserMapper.toDTO(user_repository.findById(entity.getOwner()).orElseThrow(() ->
                new DTOMappingException("Failed to map owner to from friend tag")));
        return new FriendTagDTO(
                entity.getId(),
                entity.getDisplayName(),
                entity.getColorHexCode(),
                owner,
                friends
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        FriendTag friendTag = new FriendTag();
        friendTag.setId(dto.id());
        friendTag.setDisplayName(dto.displayName());
        friendTag.setColorHexCode(dto.colorHexCode());
        friendTag.setOwner(UserMapper.toEntity(dto.owner()).getId());
        // TODO: setup later once proper relationships in entity classes are setup:
        return friendTag;
    }

    public static List<FriendTagDTO> toDTOList(List<FriendTag> entities) {
        return entities.stream()
                .map(FriendTagMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> dtos) {
        return dtos.stream()
                .map(FriendTagMapper::toEntity)
                .collect(Collectors.toList());
    }
}
