package com.danielagapov.spawn.DTOs;

import java.io.Serializable;
import java.util.UUID;

public record ChatMessageLikesDTO (
        UUID chatMessageId,
        UUID userId
) implements Serializable{}
