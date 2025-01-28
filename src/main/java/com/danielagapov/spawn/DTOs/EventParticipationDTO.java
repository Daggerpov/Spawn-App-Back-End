package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.io.Serializable;
import java.util.UUID;

public record EventParticipationDTO (
        UUID eventId,
        UUID userId,
        ParticipationStatus status
) implements Serializable {}