package com.danielagapov.spawn.DTOs.User;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public class OptionalDetailsDTO implements Serializable {
    @JsonProperty("name")
    private final String name;
    @JsonProperty("profilePictureData")
    private final byte[] profilePictureData;
}
