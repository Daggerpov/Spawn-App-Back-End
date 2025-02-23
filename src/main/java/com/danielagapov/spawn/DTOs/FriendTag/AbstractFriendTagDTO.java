package com.danielagapov.spawn.DTOs.FriendTag;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Data
public abstract class AbstractFriendTagDTO implements Serializable {
    UUID id;
    String displayName;
    String colorHexCode;
    @Getter(AccessLevel.NONE)
    boolean isEveryone;

    public boolean isEveryone() {
        return isEveryone;
    }
}
