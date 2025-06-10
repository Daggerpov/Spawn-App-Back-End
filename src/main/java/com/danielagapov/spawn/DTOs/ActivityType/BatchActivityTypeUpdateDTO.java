package com.danielagapov.spawn.DTOs.ActivityType;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class BatchActivityTypeUpdateDTO implements Serializable {
    private List<ActivityTypeDTO> updatedActivityTypes; // List of updated/newly created Activity Types
    private List<UUID> deletedActivityTypeIds; // List of Activity Type IDs that were deleted
}
