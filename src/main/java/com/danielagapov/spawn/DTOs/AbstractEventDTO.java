package com.danielagapov.spawn.DTOs;

import java.time.OffsetDateTime;
import java.util.UUID;

// Groups events under an "abstract event"
public abstract class AbstractEventDTO implements IEventDTO{
    UUID id;
    String title;
    OffsetDateTime startTime;
    OffsetDateTime endTime;
    String note;
    UUID creatorUserId;
}
