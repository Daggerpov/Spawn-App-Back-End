package com.danielagapov.spawn.DTOs.FriendTag;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public abstract class AbstractFriendTagDTO implements Serializable {
    UUID id;
    String displayName;
    String colorHexCode;
    @JsonProperty("isEveryone") // Explicitly define JSON property name
    boolean isEveryone;
}
