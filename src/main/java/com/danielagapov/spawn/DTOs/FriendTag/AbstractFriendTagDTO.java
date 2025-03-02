package com.danielagapov.spawn.DTOs.FriendTag;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public abstract class AbstractFriendTagDTO implements Serializable {
    private UUID id;
    private String displayName;
    private String colorHexCode;
}
