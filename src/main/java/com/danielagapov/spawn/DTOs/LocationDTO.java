package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.UUID;

public record LocationDTO(

        UUID id,
        String name,
        double latitude,
        double longitude

) implements Serializable {}