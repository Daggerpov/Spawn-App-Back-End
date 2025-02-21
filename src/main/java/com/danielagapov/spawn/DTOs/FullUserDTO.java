package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FullUserDTO extends AbstractUserDTO implements Serializable, IOnboardedUserDTO {
    List<FullUserDTO> friends;
    String username;
    String bio;
    List<FullFriendTagDTO> friendTags;
}