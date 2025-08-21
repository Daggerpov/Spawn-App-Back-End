package com.danielagapov.spawn.DTOs.ErrorLog;

import com.danielagapov.spawn.Enums.ResolutionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogDTO {
    private UUID id;
    private String errorMessage;
    private String stackTrace;
    private String sourceClass;
    private String sourceMethod;
    private Integer lineNumber;
    private String errorLevel;
    private Instant occurredAt;
    private Instant resolvedAt;
    private ResolutionStatus status;
    private String adminComment;
    private Boolean emailSent;
    private String userContext;
}

