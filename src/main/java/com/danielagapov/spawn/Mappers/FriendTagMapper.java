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
                UserMapper.toDTO(entity.getOwner(), uft_repository, user_repository),
                UserMapper.toDTOList(entity.getFriends(), uft_repository, user_repository)
        );
    }

    public static FriendTag toEntity(FriendTagDTO dto) {
        FriendTag friendTag = new FriendTag();
        friendTag.setId(dto.id());
        friendTag.setDisplayName(dto.displayName());
        friendTag.setColorHexCode(dto.colorHexCode());
        friendTag.setOwner(UserMapper.toEntity(dto.owner()));
        // TODO: setup later once proper relationships in entity classes are setup:
        return friendTag;
    }

    public static List<FriendTagDTO> toDTOList(List<FriendTag> entities, IUserFriendTagRepository uft_repository, IUserRepository user_repository) {
        return entities.stream()
                .map(friendTag -> toDTO(friendTag, uft_repository, user_repository))
                .collect(Collectors.toList());
    }

    public static List<FriendTag> toEntityList(List<FriendTagDTO> dtos) {
        return dtos.stream()
                .map(FriendTagMapper::toEntity)
                .collect(Collectors.toList());
    }
}
