package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.ParticipationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class EventParticipationDTO implements Serializable {
    UUID eventId;
    UUID userId;
    ParticipationStatus status;
}