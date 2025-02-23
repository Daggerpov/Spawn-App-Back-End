package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class LocationDTO implements Serializable {
    UUID id;
    String name;
    double latitude;
    double longitude;
}