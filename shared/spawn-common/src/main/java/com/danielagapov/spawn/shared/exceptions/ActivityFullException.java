package com.danielagapov.spawn.shared.exceptions;

import java.util.UUID;

public class ActivityFullException extends RuntimeException {
    private final UUID activityId;
    private final Integer participantLimit;
    
    public ActivityFullException(UUID activityId, Integer participantLimit) {
        super(String.format("Activity %s is full. Maximum participants: %d", activityId, participantLimit));
        this.activityId = activityId;
        this.participantLimit = participantLimit;
    }
    
    public UUID getActivityId() {
        return activityId;
    }
    
    public Integer getParticipantLimit() {
        return participantLimit;
    }
} 