package com.danielagapov.spawn.DTOs.ErrorLog;

import com.danielagapov.spawn.Enums.ResolutionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateErrorLogStatusDTO {
    private ResolutionStatus status;
    private String adminComment;
}

