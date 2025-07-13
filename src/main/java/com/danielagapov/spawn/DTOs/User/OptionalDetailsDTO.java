package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class OptionalDetailsDTO implements Serializable {
    private final String name;
    private final byte[] profilePictureData;
}
