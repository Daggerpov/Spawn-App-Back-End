package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.FriendTag.FriendTag;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        /*
        // TODO: change this once relations are setup within entities:
        List<FriendTagDTO> friendTagDTOs = user.getFriendTags().stream()
                .map(friendTag -> new FriendTagDTO(
                        friendTag.getId(),
                        friendTag.getDisplayName(),
                        friendTag.getColor(),
                        null
                ))
                .collect(Collectors.toList());
         */

        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getProfilePicture(),
                null
//                friendTagDTOs
        );
    }

    public static User toEntity(UserDTO dto) {
        // TODO: enable this once relations are setup within entities:
//        List<FriendTag> friendTags = dto.friendTags().stream()
//                .map(friendTagDTO -> new FriendTag(
//                        friendTagDTO.id(),
//                        friendTagDTO.displayName(),
//                        friendTagDTO.color()
//                ))
//                .collect(Collectors.toList());

        User user = new User();
        user.setId(dto.id());
        user.setUsername(dto.username());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setBio(dto.bio());
        user.setProfilePicture(dto.profilePicture());
        // TODO: once relations are setup within entities:
//        user.setFriendTags(friendTags); // Set the friend tags
        return user;
    }
}
