package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class FullFriendTagDTO implements Serializable, IFriendTagDTO {
    UUID id;
    String displayName;
    String colorHexCode;
    List<UserDTO> friends;
    boolean isEveryone;
}