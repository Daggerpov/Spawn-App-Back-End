package com.danielagapov.spawn.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OptionalDetailsDTO implements Serializable {
    private String name;
    private byte[] profilePictureData;
}
