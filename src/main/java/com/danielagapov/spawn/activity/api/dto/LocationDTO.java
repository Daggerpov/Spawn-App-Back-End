package com.danielagapov.spawn.activity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LocationDTO implements Serializable {
    UUID id;
    String name;
    double latitude;
    double longitude;
}