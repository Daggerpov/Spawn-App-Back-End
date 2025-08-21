package com.danielagapov.spawn.DTOs.ErrorLog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateErrorLogDTO {
    private String errorMessage;
    private String stackTrace;
    private String sourceClass;
    private String sourceMethod;
    private Integer lineNumber;
    private String errorLevel;
    private String userContext;
}

