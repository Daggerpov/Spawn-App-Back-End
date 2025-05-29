package com.danielagapov.spawn.DTOs.Activity;

import com.danielagapov.spawn.Enums.ParticipationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ActivityParticipationDTO implements Serializable {
    UUID activityId;
    UUID userId;
    ParticipationStatus status;
}