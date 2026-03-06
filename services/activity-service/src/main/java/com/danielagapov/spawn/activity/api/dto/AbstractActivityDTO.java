package com.danielagapov.spawn.activity.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

@NoArgsConstructor
@Getter
@Setter
public abstract class AbstractActivityDTO implements Serializable {
    UUID id;
    String title;
    OffsetDateTime startTime;
    OffsetDateTime endTime;
    String note;
    String icon;
    Integer participantLimit;
    Instant createdAt;
    
    @JsonProperty("isExpired")
    boolean isExpired;
    
    String clientTimezone;

    public AbstractActivityDTO(UUID id, String title, OffsetDateTime startTime, OffsetDateTime endTime, 
                              String note, String icon, Integer participantLimit, Instant createdAt, boolean isExpired, String clientTimezone) {
        this.id = id; this.title = title; this.startTime = startTime; this.endTime = endTime;
        this.note = note; this.icon = icon; this.participantLimit = participantLimit;
        this.createdAt = createdAt; this.isExpired = isExpired; this.clientTimezone = clientTimezone;
    }
}
