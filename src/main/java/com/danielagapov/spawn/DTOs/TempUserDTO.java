package com.danielagapov.spawn.DTOs;

import java.io.Serializable;

public record TempUserDTO(
        String id,
        String firstName,
        String lastName,
        String email,
        String profilePicture
) implements Serializable, AbstractUserDTO {}
